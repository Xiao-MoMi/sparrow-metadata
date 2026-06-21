package net.momirealms.sparrow.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 跨服元数据值的抽象基类，提供 get / lastKnownValue / update 的通用实现，
 * 消除 CrossServer*MetaDataValue 之间的重复代码并修复并发问题。
 *
 * @param <T> 元数据值的类型
 */
public abstract class AbstractCrossServerMetaDataValue<T> implements CrossServerMetaDataValue<T> {
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractCrossServerMetaDataValue.class);

    protected final MetaDataUser user;
    protected final MetaData<T, ? extends MetaDataValue<T>> metaData;
    protected T lastKnownValue;
    protected long lastUpdateTime;
    private CompletableFuture<T> cachedFuture;

    protected AbstractCrossServerMetaDataValue(MetaDataUser user, MetaData<T, ? extends MetaDataValue<T>> metaData) {
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

    @Override
    public CompletableFuture<T> get() {
        return this.user.repository().get(this.metaData).thenApply(it -> {
            this.lastKnownValue = it;
            this.lastUpdateTime = System.nanoTime();
            return it;
        });
    }

    /**
     * 返回最近一次已知的值，避免重复查询数据库。
     * 使用 synchronized 防止 check-then-act 竞态导致重复数据库请求。
     */
    @Override
    public synchronized CompletableFuture<T> lastKnownValue() {
        if (this.lastUpdateTime != 0) {
            return CompletableFuture.completedFuture(this.lastKnownValue);
        } else {
            if (this.cachedFuture == null) {
                this.cachedFuture = this.get();
            }
            return this.cachedFuture;
        }
    }

    /**
     * 跨服更新，使用时间戳进行 last-write-wins 冲突解决。
     */
    @Override
    public CompletableFuture<Boolean> update(T value, long time, boolean markForSave) {
        if (!updateLastKnownValue(value, time)) {
            return CompletableFuture.completedFuture(false);
        }
        if (markForSave) {
            return this.user.repository()
                    .update(Map.of(collection(), List.of(new FriendlyData<>(this.metaData.id(), this.metaData.dataType(), value))))
                    .handle((__, t) -> {
                        if (t == null) {
                            publishUpdate(value, time);
                            return true;
                        }
                        LOGGER.error("Error updating cross server metadata {} for player {}", this.metaData.id(), this.user.uuid(), t);
                        return false;
                    });
        }
        return CompletableFuture.completedFuture(true);
    }

    protected boolean updateLastKnownValue(T value, long time) {
        if (this.lastUpdateTime <= time) {
            this.lastKnownValue = value;
            this.lastUpdateTime = time;
            return true;
        } else {
            return false;
        }
    }

    protected void publishUpdate(T value, long time) {
        this.user.manager().messageBroker().publishOneWay(
                new BroadcastCrossServerMetadataValueMessage(this.user.uuid(), this.metaData.id(), this.metaData.dataType().encode(value), time), "");
    }
}
