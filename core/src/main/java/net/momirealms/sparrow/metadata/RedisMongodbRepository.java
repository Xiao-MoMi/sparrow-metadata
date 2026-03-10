package net.momirealms.sparrow.metadata;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import io.lettuce.core.RedisClient;
import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 基于 Redis (缓存) 和 MongoDB (持久化) 的元数据仓库实现
 * 采用 Cache-Aside 模式：先更新数据库，再删除缓存，确保最终一致性
 */
public final class RedisMongodbRepository implements PersistentRepository, AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisMongodbRepository.class);
    private static final UpdateOptions DEFAULT_UPSERT_OPTIONS = new UpdateOptions().upsert(true);
    private static final byte[] NULL_MARKER = "##NULL##".getBytes(StandardCharsets.UTF_8);
    private static final long CACHE_TTL_SECONDS = Duration.ofMinutes(10).toSeconds();
    private static final long LOCK_SECONDS = 60;

    private final MongoClient mongoClient;
    private final MongoDatabase mongoDatabase;
    private final String defaultCollectionName;
    private final Map<String, MongoCollection<Document>> mongoCollections;

    private final StatefulRedisConnection<byte[], byte[]> redisConnection;
    private final RedisCommands<byte[], byte[]> redisCommands;

    private final String cachePrefix;
    private final ExecutorService executor;

    /**
     * 构造一个新的元数据仓库。
     * <p>注意资源所有权约定：</p>
     * <ul>
     * <li><b>MongoClient:</b> 由外部传入并共享，本类不会在 {@link #close()} 时关闭它。请确保在应用结束时由外部调用者关闭。</li>
     * <li><b>RedisClient:</b> 仅用于建立连接，本类持有的具体 {@code redisConnection} 会在 {@link #close()} 时释放。</li>
     * <li><b>ExecutorService:</b> 本类私有的虚拟线程池，会在 {@link #close()} 时自动关闭。</li>
     * </ul>
     *
     * @param redisClient           Redis 客户端（外部管理生命周期）
     * @param mongoClient           MongoDB 客户端（外部管理生命周期）
     * @param databaseName          MongoDB 数据库名称
     * @param defaultCollectionName 默认集合名称
     * @param cachePrefix           Redis 缓存键前缀
     */
    public RedisMongodbRepository(RedisClient redisClient,
                                  MongoClient mongoClient,
                                  String databaseName,
                                  String defaultCollectionName,
                                  String cachePrefix) {
        this.mongoClient = mongoClient;
        this.mongoDatabase = this.mongoClient.getDatabase(databaseName);
        this.defaultCollectionName = defaultCollectionName;
        this.mongoCollections = new HashMap<>();

        // 初始化默认集合
        this.mongoCollections.put(defaultCollectionName, this.mongoDatabase.getCollection(defaultCollectionName));

        this.redisConnection = redisClient.connect(ByteArrayCodec.INSTANCE);
        this.redisCommands = this.redisConnection.sync();

        this.cachePrefix = cachePrefix;

        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    private byte[] getRedisKey(final UUID uuid, final String key) {
        return (this.cachePrefix + ":" + uuid.toString() + "_" + key).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] getLockKey(final UUID uuid) {
        return (this.cachePrefix + "_lock:" + uuid.toString()).getBytes(StandardCharsets.UTF_8);
    }

    private MongoCollection<Document> getCollection(final String collection) {
        if (collection == null) {
            return this.mongoCollections.get(defaultCollectionName);
        }
        return this.mongoCollections.computeIfAbsent(collection, this.mongoDatabase::getCollection);
    }

    private MongoCollection<Document> getCollection(final MetaData<?, ?> metaData) {
        return getCollection(metaData.collection());
    }

    @Override
    public String defaultCollectionName() {
        return this.defaultCollectionName;
    }

    @Override
    public <T> CompletableFuture<T> get(UUID uuid, MetaData<T, ?> metaData) {
        return CompletableFuture.supplyAsync(() -> {
            String fieldName = metaData.id();
            byte[] redisKey = getRedisKey(uuid, fieldName);

            // 1. 尝试从 Redis 读取
            try {
                byte[] data = this.redisCommands.get(redisKey);
                if (data != null) {
                    if (Arrays.equals(data, NULL_MARKER)) return null;
                    return metaData.dataType().decode(data);
                }
            } catch (Exception e) {
                LOGGER.warn("Redis read failed for {}, fallback to DB", uuid, e);
            }

            // 2. Redis 未命中，从 MongoDB 读取
            Document doc = getCollection(metaData).find(Filters.eq("_id", uuid))
                    .projection(Projections.include(fieldName))
                    .first();

            if (doc != null && doc.containsKey(fieldName)) {
                Object fieldValue = doc.get(fieldName);
                if (fieldValue != null) {
                    T parsed = metaData.dataType().parse(fieldValue);
                    if (parsed != null) {
                        // 回填缓存
                        safeSetCache(redisKey, metaData.dataType().encode(parsed));
                    } else {
                        safeSetCache(redisKey, NULL_MARKER);
                    }
                    return parsed;
                }
            }

            // 3. 数据库也没有，设置空值标记防止缓存穿透
            safeSetCache(redisKey, NULL_MARKER);
            return null;
        }, this.executor);
    }

    @Override
    public CompletableFuture<Instant> getExpiryTime(final UUID uuid, final String metaDataKey) {
        return CompletableFuture.supplyAsync(() -> {
            String fieldName = metaDataKey + ExpirableMetaDataValue.SUFFIX;
            byte[] redisKey = getRedisKey(uuid, fieldName);

            try {
                byte[] data = this.redisCommands.get(redisKey);
                if (data != null) {
                    if (Arrays.equals(data, NULL_MARKER)) return null;
                    return Util.decodeInstant(data);
                }
            } catch (Exception e) {
                LOGGER.warn("Redis read failed for expiry {}, fallback to DB", uuid, e);
            }

            Document doc = getCollection(metaDataKey).find(Filters.eq("_id", uuid))
                    .projection(Projections.include(fieldName))
                    .first();

            if (doc != null && doc.containsKey(fieldName)) {
                Object rawTime = doc.get(fieldName);
                if (rawTime instanceof Instant time) {
                    safeSetCache(redisKey, Util.encodeInstant(time));
                    return time;
                }
            }

            safeSetCache(redisKey, NULL_MARKER);
            return null;
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> update(UUID uuid, Map<String, List<FriendlyData<?>>> toUpdate) {
        return CompletableFuture.runAsync(() -> {
            if (toUpdate == null || toUpdate.isEmpty()) return;

            // 1. 更新 MongoDB
            for (Map.Entry<String, List<FriendlyData<?>>> entry : toUpdate.entrySet()) {
                MongoCollection<Document> collection = getCollection(entry.getKey());
                List<Bson> updates = new ArrayList<>();
                for (FriendlyData<?> dataEntry : entry.getValue()) {
                    Object data = dataEntry.data();
                    Object processedData = convertToMongoCompatible(data);
                    updates.add(Updates.set(dataEntry.storageId(), processedData));
                }

                if (!updates.isEmpty()) {
                    collection.updateOne(
                            Filters.eq("_id", uuid),
                            Updates.combine(updates),
                            DEFAULT_UPSERT_OPTIONS
                    );
                }
            }

            // 2. 删除 Redis 缓存 (Cache-Aside 策略)
            // 采用删除而非更新，可以有效避免并发下的数据脏读
            List<byte[]> keysToDelete = new ArrayList<>();
            for (List<FriendlyData<?>> dataList : toUpdate.values()) {
                for (FriendlyData<?> dataEntry : dataList) {
                    keysToDelete.add(getRedisKey(uuid, dataEntry.storageId()));
                }
            }

            if (!keysToDelete.isEmpty()) {
                try {
                    this.redisCommands.del(keysToDelete.toArray(new byte[0][]));
                } catch (Exception e) {
                    LOGGER.error("Failed to delete Redis cache after DB update for {}", uuid, e);
                }
            }
        }, this.executor);
    }

    private Object convertToMongoCompatible(Object data) {
        if (data instanceof long[] longs) {
            List<Long> list = new ArrayList<>(longs.length);
            for (long l : longs) list.add(l);
            return list;
        }
        if (data instanceof int[] ints) {
            List<Integer> list = new ArrayList<>(ints.length);
            for (int i : ints) list.add(i);
            return list;
        }
        return data;
    }

    @Override
    public CompletableFuture<Map<String, Object>> getAll(UUID uuid, String[] collections) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> result = new HashMap<>();
            for (String colName : collections) {
                Document doc = getCollection(colName).find(Filters.eq("_id", uuid)).first();
                if (doc != null) {
                    for (Map.Entry<String, Object> entry : doc.entrySet()) {
                        Object value = entry.getValue();
                        // 统一转换 Binary 为 byte[]
                        if (value instanceof Binary binary) {
                            result.put(entry.getKey(), binary.getData());
                        }
                        // 兼容处理：将 List 转回对应的基本类型数组
                        else if (value instanceof List<?> list && !list.isEmpty()) {
                            result.put(entry.getKey(), convertListToArray(list));
                        }
                        else {
                            result.put(entry.getKey(), value);
                        }
                    }
                }
            }
            return result;
        }, this.executor);
    }

    private Object convertListToArray(List<?> list) {
        Object first = list.getFirst();
        if (first instanceof Long) {
            long[] arr = new long[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = (long) list.get(i);
            return arr;
        }
        if (first instanceof Integer) {
            int[] arr = new int[list.size()];
            for (int i = 0; i < list.size(); i++) arr[i] = (int) list.get(i);
            return arr;
        }
        return list;
    }

    @Override
    public CompletableFuture<Byte> increaseAndGet(UUID uuid, MetaData<Byte, ?> metaData, byte value) {
        return increaseAndGetInternal(uuid, metaData, value, Number::byteValue);
    }

    @Override
    public CompletableFuture<Double> increaseAndGet(UUID uuid, MetaData<Double, ?> metaData, double value) {
        return increaseAndGetInternal(uuid, metaData, value, Number::doubleValue);
    }

    @Override
    public CompletableFuture<Integer> increaseAndGet(UUID uuid, MetaData<Integer, ?> metaData, int value) {
        return increaseAndGetInternal(uuid, metaData, value, Number::intValue);
    }

    @Override
    public CompletableFuture<Long> increaseAndGet(UUID uuid, MetaData<Long, ?> metaData, long value) {
        return increaseAndGetInternal(uuid, metaData, value, Number::longValue);
    }

    /**
     * 利用 MongoDB $inc 原子性实现
     */
    private <T extends Number> CompletableFuture<T> increaseAndGetInternal(
            UUID uuid,
            MetaData<T, ?> metaData,
            Number value,
            Function<Number, T> converter) {

        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> collection = getCollection(metaData);
            String fieldName = metaData.id();

            // 利用 Mongo 原子更新并返回新文档
            List<Bson> pipeline = List.of(
                    Updates.set(fieldName,
                            new Document("$add", Arrays.asList(
                                    new Document("$ifNull", Arrays.asList("$" + fieldName, 0)),
                                    value
                            ))
                    )
            );

            Document result = collection.findOneAndUpdate(
                    Filters.eq("_id", uuid),
                    pipeline, // 传入管道而不是简单的 Updates 对象
                    new FindOneAndUpdateOptions()
                            .upsert(true)
                            .returnDocument(ReturnDocument.AFTER)
                            .projection(Projections.include(fieldName))
            );

            if (result == null) {
                throw new IllegalStateException("Atomic update failed for " + uuid);
            }

            T afterValue = converter.apply(result.get(fieldName, Number.class));

            // 移除旧缓存，强制下次 get 时拉取最新值
            try {
                this.redisCommands.del(getRedisKey(uuid, fieldName));
            } catch (Exception e) {
                LOGGER.warn("Failed to clear cache after atomic increment", e);
            }

            return afterValue;
        }, this.executor);
    }

    @Override
    public CompletableFuture<Byte> decreaseAndGet(UUID uuid, MetaData<Byte, ?> metaData, byte value, boolean checkBalance) {
        return decreaseAndGetInternal(uuid, metaData, value, -value, Number::byteValue, checkBalance);
    }

    @Override
    public CompletableFuture<Double> decreaseAndGet(UUID uuid, MetaData<Double, ?> metaData, double value, boolean checkBalance) {
        return decreaseAndGetInternal(uuid, metaData, value, -value, Number::doubleValue, checkBalance);
    }

    @Override
    public CompletableFuture<Integer> decreaseAndGet(UUID uuid, MetaData<Integer, ?> metaData, int value, boolean checkBalance) {
        return decreaseAndGetInternal(uuid, metaData, value, -value, Number::intValue, checkBalance);
    }

    @Override
    public CompletableFuture<Long> decreaseAndGet(UUID uuid, MetaData<Long, ?> metaData, long value, boolean checkBalance) {
        return decreaseAndGetInternal(uuid, metaData, value, -value, Number::longValue, checkBalance);
    }

    /**
     * 利用 MongoDB 原子更新实现带条件的数值扣减。
     * 约束：数据库中的当前值必须 >= 要扣除的数值。
     */
    private <T extends Number> CompletableFuture<T> decreaseAndGetInternal(
            UUID uuid,
            MetaData<T, ?> metaData,
            Number value,
            Number decValue,
            Function<Number, T> converter,
            boolean checkBalance) {

        return CompletableFuture.supplyAsync(() -> {
            MongoCollection<Document> collection = getCollection(metaData);
            String fieldName = metaData.id();

            Bson filter = Filters.eq("_id", uuid);
            if (checkBalance) {
                // 如果检查余额，增加 gte 条件
                filter = Filters.and(filter, Filters.gte(fieldName, value));
            }

            FindOneAndUpdateOptions options = new FindOneAndUpdateOptions()
                    .returnDocument(ReturnDocument.AFTER)
                    .projection(Projections.include(fieldName));

            // 如果不检查余则允许负数
            if (!checkBalance) {
                options.upsert(true);
            }

            // 执行原子更新
            Document result = collection.findOneAndUpdate(filter, Updates.inc(fieldName, decValue), options);

            // 如果 result 为 null，仅在 checkBalance=true 时表示余额不足
            // 若 upsert=true，result 理论上不会为 null
            if (result == null) {
                return null;
            }

            T afterValue = converter.apply(result.get(fieldName, Number.class));

            try {
                this.redisCommands.del(getRedisKey(uuid, fieldName));
            } catch (Exception e) {
                LOGGER.warn("Failed to clear cache after atomic decrement for {}", uuid, e);
            }

            return afterValue;
        }, this.executor);
    }

    private void safeSetCache(byte[] key, byte[] value) {
        try {
            this.redisCommands.setex(key, CACHE_TTL_SECONDS, value);
        } catch (Exception e) {
            LOGGER.warn("Failed to write to Redis cache", e);
        }
    }

    @Override
    public void close() {
        try {
            this.redisConnection.close();
            this.executor.shutdown();
        } catch (Exception e) {
            LOGGER.error("Error closing Repository resources", e);
        }
    }

    @Override
    public CompletableFuture<Void> lock(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            this.redisCommands.setex(getLockKey(uuid), LOCK_SECONDS, NULL_MARKER);
            return null;
        }, this.executor);
    }

    @Override
    public CompletableFuture<Void> unlock(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            this.redisCommands.del(getLockKey(uuid));
            return null;
        }, this.executor);
    }

    @Override
    public boolean acquireLock(UUID uuid, Supplier<Boolean> onlineIndicator) {
        SetArgs args = SetArgs.Builder.nx().px(Duration.ofSeconds(LOCK_SECONDS));
        long currentTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - currentTime < 10_000) {
            if (!onlineIndicator.get()) {
                return false;
            }
            String ok = this.redisCommands.set(getLockKey(uuid), NULL_MARKER, args);
            if ("OK".equals(ok)) {
                return true;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }
}