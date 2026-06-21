package net.momirealms.sparrow.metadata;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ExpirableCommonMetaDataValue<T> implements LazilyPersistedMetaDataValue<T>, ExpirableMetaDataValue<T> {
    protected final MetaDataUser user;
    protected final ExpirableCommonMetaData<T> metaData;
    protected boolean valueChanged;
    protected boolean timeChanged;
    protected T cachedValue;
    protected long lastOfflineUpdateTime;
    protected CompletableFuture<T> cachedFuture;
    protected Instant expiryTime; // 如果值是 -1 代表未加载
    protected ReentrantLock lock = new ReentrantLock();

    protected ExpirableCommonMetaDataValue(MetaDataUser user, ExpirableCommonMetaData<T> metaData) {
        this.user = user;
        this.metaData = metaData;
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
        return getCachedValueIgnoreLoaded();
    }

    public T getCachedValueOrDefault(T defaultValue) {
        if (!this.user.loaded()) {
            throw new IllegalStateException("User " + this.user.uuid() + " is not online");
        }
        return Optional.ofNullable(getCachedValueIgnoreLoaded()).orElse(defaultValue);
    }

    public T getCachedValue(Supplier<T> defaultValue) {
        if (!this.user.loaded()) {
            throw new IllegalStateException("User " + this.user.uuid() + " is not online");
        }
        return Optional.ofNullable(getCachedValueIgnoreLoaded()).orElseGet(defaultValue);
    }

    private T getCachedValueIgnoreLoaded() {
        if (this.expiryTime == null || Instant.now().isAfter(this.expiryTime)) {
            return null;
        } else {
            return this.cachedValue;
        }
    }

    /**
     * 强制保存为某值，使用 {@link Instant#MAX} 作为默认过期时间（永不过期）。
     * 用于管理指令等需要直接覆盖的场景。
     *
     * @param value 值
     */
    @Override
    public CompletableFuture<Void> forceUpdate(T value) {
        return forceUpdate(value, Instant.MAX);
    }

    /**
     * 强制保存为某值，不需要在乎玩家是否在线
     * <p>
     * 这里会存在一个问题：玩家上线但是数据未加载完全，中途被forceUpdate。一般来说不考虑此情况
     *
     * @param value 值
     * @param time  过期时间
     */
    public CompletableFuture<Void> forceUpdate(T value, Instant time) {
        // 玩家在当前服务器，那么只需要更新这里的cache
        if (this.user.loaded()) {
            this.cachedValue = value;
            this.expiryTime = time;
            return this.user.repository()
                    .update(Map.of(this.collection(), List.of(
                            new FriendlyData<>(this.metaData.id, this.metaData.dataType, value),
                            new FriendlyData<>(this.metaData.id + SUFFIX, DataType.INSTANT, time)
                    )));
        } else {
            // 不在当前服务器，就可能在其他服务器上，发送redis消息以同步
            return this.user.repository()
                    .update(Map.of(this.collection(), List.of(
                            new FriendlyData<>(this.metaData.id, this.metaData.dataType, value),
                            new FriendlyData<>(this.metaData.id + SUFFIX, DataType.INSTANT, time)
                    )))
                    .thenRun(() -> {
                        this.user.manager().messageBroker().publishOneWay(new BroadcastMetadataExpirableValueMessage(this.user.uuid(), this.metaData.id, this.metaData.dataType.encode(value), time), "");
                    });
        }
    }

    @Override
    public CompletableFuture<Boolean> update(T value, Instant expiryTime, boolean markForSave) {
        if (markForSave) {
            return ExpirableMetaDataValue.super.update(value, expiryTime, true);
        } else {
            this.lock.lock();
            try {
                this.cachedValue = value;
                this.expiryTime = expiryTime;
                // 如果玩家没加载完全，那么更新一下cached future
                // 1. 离线，收到其他服务器的更新
                // 2. 正在加载中，处于进服过程的更新
                if (!this.user.loaded()) {
                    this.cachedFuture = CompletableFuture.completedFuture(getCachedValueIgnoreLoaded());
                    this.lastOfflineUpdateTime = System.currentTimeMillis();
                }
                return CompletableFuture.completedFuture(true);
            } finally {
                this.lock.unlock();
            }
        }
    }

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
                this.valueChanged = true;
            }
            // 更新缓存值
            this.cachedValue = value;
            // 如果玩家没加载完全，那么就标记离线数据无效
            // 1. 离线，收到其他服务器的更新
            // 2. 正在加载中，处于进服过程的更新
            if (!this.user.loaded()) {
                this.lastOfflineUpdateTime = 0;
                this.cachedFuture = null;
            }
        } finally {
            this.lock.unlock();
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Boolean> update(Instant time, boolean markForSave) {
        this.lock.lock();
        try {
            // 如果新值与当前缓存值相同，则无需更新
            if (Objects.equals(time, this.expiryTime)) {
                return CompletableFuture.completedFuture(false);
            }
            // 根据参数决定是否标记为需要保存
            if (markForSave) {
                this.timeChanged = true;
            }
            // 更新缓存值
            this.expiryTime = time;
            // 如果玩家没加载完全，那么就标记离线数据无效
            // 1. 离线，收到其他服务器的更新
            // 2. 正在加载中，处于进服过程的更新
            if (!this.user.loaded()) {
                this.lastOfflineUpdateTime = 0;
                this.cachedFuture = null;
            }
        } finally {
            this.lock.unlock();
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public void saveIfDirty(Consumer<FriendlyData<?>> callback) {
        this.lock.lock();
        try {
            if (this.timeChanged) {
                Instant time = this.expiryTime;
                this.timeChanged = false;
                callback.accept(new FriendlyData<>(this.metaData.id + SUFFIX, DataType.INSTANT, time));
            }
            if (this.valueChanged) {
                T cached = this.cachedValue;
                this.valueChanged = false;
                callback.accept(new FriendlyData<>(this.metaData.id, this.metaData.dataType, cached));
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public CompletableFuture<T> get() {
        // 如果已初始化且玩家在线，直接返回缓存值
        if (this.user.loaded()) {
            if (this.expiryTime == null || Instant.now().isAfter(this.expiryTime)) {
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.completedFuture(this.cachedValue);
            }
        }

        // 离线玩家或正在加载中的玩家
        long current = System.currentTimeMillis();

        this.lock.lock();
        try {
            // 如果没有过离线请求
            if (this.cachedFuture == null) {
                this.lastOfflineUpdateTime = current;
                this.cachedFuture = getExpirableData();
                return this.cachedFuture;
            }

            // 如果已经10秒了，需要强制刷新一次future
            if (current - this.lastOfflineUpdateTime > 10_000) {
                this.cachedFuture = getExpirableData();
                this.lastOfflineUpdateTime = current;
            }

            // 否则用旧的
            return this.cachedFuture;
        } finally {
            this.lock.unlock();
        }
    }

    private CompletableFuture<T> getExpirableData() {
        return getData().thenCombine(getExpiryTime(), (t, expiryTime) -> {
            if (expiryTime == null || Instant.now().isAfter(expiryTime)) {
                return null;
            }
            return t;
        });
    }

    private CompletableFuture<Instant> getExpiryTime() {
        return this.user.repository().getExpiryTime(this.metaData.id, this.collection());
    }

    private CompletableFuture<T> getData() {
        return this.user.repository().get(this.metaData);
    }

    @Override
    public Instant expiryTime() {
        return this.expiryTime;
    }
}
