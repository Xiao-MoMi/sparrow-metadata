package net.momirealms.sparrow.metadata;

import com.google.common.collect.MapMaker;
import net.momirealms.sparrow.redis.messagebroker.MessageBroker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@SuppressWarnings("rawtypes")
final class MetaDataManagerImpl implements MetaDataManager {
    // 持久化仓库
    private final PersistentRepository repository;
    private final MessageBroker messageBroker;
    // 注册的元数据，每次修改都会设置dirty
    private final Map<String, MetaData> metaData = new HashMap<>();
    private boolean dirty1;
    private MetaData[] metaDataArray;
    private boolean dirty2;
    private String[] collections;

    // 都是线程安全的实现
    private final Map<UUID, MetaDataUser> onlineUsers = new ConcurrentHashMap<>();
    private final Map<UUID, MetaDataUser> weakUsers = new MapMaker()
            .weakValues()
            .concurrencyLevel(4)
            .makeMap();

    MetaDataManagerImpl(PersistentRepository repository, MessageBroker messageBroker) {
        MetaDataProvider.set(this);
        this.repository = repository;
        this.messageBroker = messageBroker;
        this.messageBroker.registry().register(BroadcastMetadataValueMessage.ID, BroadcastMetadataValueMessage.CODEC);
        this.messageBroker.registry().register(BroadcastMetadataExpirableValueMessage.ID, BroadcastMetadataExpirableValueMessage.CODEC);
        this.messageBroker.registry().register(BroadcastCrossServerMetadataValueMessage.ID, BroadcastCrossServerMetadataValueMessage.CODEC);
        this.messageBroker.subscribe();
    }

    @Override
    public void registerMetaData(final MetaData metaData) {
        if (this.metaData.containsKey(metaData.id())) {
            throw new IllegalArgumentException("MetaData with the same id already registered: " + metaData.id());
        }
        this.metaData.put(metaData.id(), metaData);
        this.dirty1 = true;
        this.dirty2 = true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public MetaData getMetaData(@NotNull String id) {
        return this.metaData.get(id);
    }

    @Override
    public MetaData[] registeredMetaData() {
        if (this.dirty1 || this.metaDataArray == null) {
            this.metaDataArray = this.metaData.values().toArray(new MetaData[0]);
            this.dirty1 = false;
        }
        return this.metaDataArray;
    }

    @Override
    public String[] collections() {
        if (this.dirty2 || this.collections == null) {
            Set<String> collectionsSet = new HashSet<>();
            collectionsSet.add(this.repository.defaultCollectionName());
            for (MetaData metaData : this.registeredMetaData()) {
                String collection = metaData.collection();
                if (collection != null) {
                    collectionsSet.add(collection);
                }
            }
            this.collections = collectionsSet.toArray(new String[0]);
            this.dirty2 = false;
        }
        return this.collections;
    }

    @Override
    public @NotNull MetaDataUser getUser(final UUID uuid) {
        // 检查弱引用用户
        MetaDataUser weakUser = this.weakUsers.get(uuid);
        if (weakUser != null) {
            return weakUser;
        }

        //创建新用户
        return this.weakUsers.computeIfAbsent(uuid, k -> new MetaDataUserImpl(this, uuid));
    }

    @Override
    public @Nullable MetaDataUser getOptionalUser(final UUID uuid) {
        return this.weakUsers.get(uuid);
    }

    @Nullable
    @Override
    public MetaDataUser createOnlineUser(final UUID uuid, Supplier<Boolean> onlineIndicator, boolean acquireLock) {
        // 获取锁
        if (acquireLock && !this.repository.acquireLock(uuid, onlineIndicator)) {
            return null;
        }
        // 确保在线用户存在且只有一个实例
        MetaDataUser metaDataUser = this.onlineUsers.computeIfAbsent(uuid, k -> {
            // 如果在线用户不存在，从weakUsers获取或创建
            MetaDataUser existingUser = this.weakUsers.get(k);
            if (existingUser != null) {
                // 用户已存在于weakUsers，确保也在weakUsers中（弱引用）
                return existingUser;
            }
            MetaDataUser newUser = new MetaDataUserImpl(this, k);
            this.weakUsers.put(k, newUser);
            return newUser;
        });
        metaDataUser.setOnline(true);
        return metaDataUser;
    }

    @Override
    public CompletableFuture<Void> removeOnlineUser(final UUID uuid, boolean saveDirtyData) {
        // 从在线用户中移除
        MetaDataUser removed = this.onlineUsers.remove(uuid);
        if (removed != null) {
            removed.setOnline(false);
            if (saveDirtyData) {
                return removed.saveDirty().whenComplete((v, e) -> {
                    if (e != null) {
                        LOGGER.warn("Failed to save online user: {}", uuid, e);
                    }
                    this.repository.unlock(uuid);
                });
            } else {
                return this.repository.unlock(uuid);
            }
        }
        // weakUsers中的引用会自然被GC回收，不需要手动移除
        // 如果用户还在其他地方被引用，weakUsers会保持它
        // 如果没有其他地方引用，GC会自动清理
        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    @Override
    public PersistentRepository repository() {
        return this.repository;
    }

    @Override
    public MessageBroker messageBroker() {
        return this.messageBroker;
    }
}