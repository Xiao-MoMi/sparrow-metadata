package net.momirealms.sparrow.metadata;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

/**
 * 基础的元数据值容器，用于单个服务器环境下的元数据管理
 * 此类适用于无需跨服同步的场景，确保同一玩家的元数据实例在服务器生命周期内保持唯一
 *
 * @param <T> 元数据值的类型
 */
public class CommonMetaDataValue<T> implements LazilyPersistedMetaDataValue<T> {
    protected final MetaDataUser user;
    protected final CommonMetaData<T> metaData;
    protected boolean markedForSave;
    protected T cachedValue;
    protected long lastOfflineUpdateTime;
    protected CompletableFuture<T> cachedFuture;
    protected ReentrantReadWriteLock.WriteLock lock = new ReentrantReadWriteLock().writeLock(); // 写锁，用于线程安全更新

    /**
     * 构造函数
     *
     * @param user 关联的用户对象
     * @param metaData 对应的元数据定义
     */
    protected CommonMetaDataValue(MetaDataUser user, CommonMetaData<T> metaData) {
        this.metaData = metaData;
        this.user = user;
    }

    @Override
    public MetaData<T, ? extends MetaDataValue<T>> metadata() {
        return this.metaData;
    }

    @Override
    public MetaDataUser owner() {
        return this.user;
    }

    /**
     * 强制保存为某值，不需要在乎玩家是否在线
     * <p>
     * 这里会存在一个问题：玩家上线但是数据未加载完全，中途被forceUpdate。一般来说不考虑此情况
     *
     * @param value 值
     */
    public CompletableFuture<Void> forceUpdate(T value) {
        // 玩家在当前服务器，那么只需要更新这里的cache
        if (this.user.loaded()) {
            this.cachedValue = value;
            return this.user.repository()
                    .update(Map.of(this.collection(), List.of(new FriendlyData<>(this.metaData.id, this.metaData.dataType, value))));
        } else {
            // 不在当前服务器，就可能在其他服务器上，发送redis消息以同步
            return this.user.repository()
                    .update(Map.of(this.collection(), List.of(new FriendlyData<>(this.metaData.id, this.metaData.dataType, value))))
                    .thenRun(() -> this.user.manager().messageBroker().publishOneWay(new BroadcastMetadataValueMessage(this.user.uuid(), this.metaData.id, this.metaData.dataType.encode(value)), ""));
        }
    }

    /**
     * 获取当前内存中的缓存值（仅限在线玩家）
     * 此方法要求玩家必须在线且加载，否则会抛出异常
     *
     * @return 内存中缓存的当前值
     * @throws IllegalStateException 如果玩家不在线
     */
    public T getCachedValue() {
        if (!this.user.loaded()) {
            throw new IllegalStateException("User " + this.user.uuid() + " is not online");
        }
        return this.cachedValue;
    }

    /**
     * 从数据库获取元数据值（异步操作）
     * 结果不一定最新
     *
     * @return 包含数据库查询结果的 CompletableFuture
     */
    private CompletableFuture<T> getData() {
        return this.user.repository().get(this.metaData);
    }

    @Override
    public void saveIfDirty(Consumer<FriendlyData<?>> callback) {
        this.lock.lock();
        try {
            // 如果存在缓存值且标记为需要保存，则返回该值并清除标记
            if (this.markedForSave) {
                T cached = this.cachedValue;
                this.markedForSave = false;
                callback.accept(new FriendlyData<>(this.metaData.id, this.metaData.dataType, cached));
            }
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * 更新元数据，玩家必须在线才能正常被存储
     *
     * @param value 值
     * @param markForSave 是否保存
     * @return 是否修改
     */
    @Override
    public CompletableFuture<Boolean> update(T value, boolean markForSave) {
        this.lock.lock();
        try {
            // 如果新值与当前缓存值相同，则无需更新
            if (Objects.equals(value, this.cachedValue)) {
                return CompletableFuture.completedFuture(false);
            }
            // 根据参数决定是否标记为需要保存
            if (markForSave) {
                this.markedForSave = true;
            }
            // 更新缓存值
            this.cachedValue = value;
            // 如果玩家没加载完全
            // 1. 离线，收到其他服务器的更新
            // 2. 正在加载中，处于进服过程的更新
            if (!this.user.loaded()) {
                this.cachedFuture = CompletableFuture.completedFuture(value);
                this.lastOfflineUpdateTime = System.currentTimeMillis();
            }
        } finally {
            this.lock.unlock();
        }
        return CompletableFuture.completedFuture(true);
    }

    /**
     * 获取元数据值（主入口方法）
     * 当玩家在线时，始终返回内存中的最新值
     * 当玩家离线时，获取数据不一定为最新，因为此数据类型不能保证最新
     * 你可以暴力调用此方法，短时间突发多次调用并不会导致发起多条数据库请求
     *
     * @return 包含元数据值的 CompletableFuture
     */
    @Override
    public synchronized CompletableFuture<T> get() {
        // 如果已初始化且玩家在线，直接返回缓存值
        if (this.user.loaded()) {
            return CompletableFuture.completedFuture(this.cachedValue);
        }

        // 正在加载中 或 不在线，则直接取最新值

        long current = System.currentTimeMillis();

        // 如果没有过离线请求
        if (this.cachedFuture == null) {
            this.lastOfflineUpdateTime = current;
            this.cachedFuture = getData();
            return this.cachedFuture;
        }

        // 如果已经10秒了，需要强制刷新一次future
        if (current - this.lastOfflineUpdateTime > 10_000) {
            this.cachedFuture = getData();
            this.lastOfflineUpdateTime = current;
        }

        // 否则用旧的
        return this.cachedFuture;
    }
}