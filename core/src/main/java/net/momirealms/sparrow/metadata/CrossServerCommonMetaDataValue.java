package net.momirealms.sparrow.metadata;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrossServerCommonMetaDataValue<T> implements CrossServerMetaDataValue<T> {
    protected final MetaDataUser user;
    protected final CrossServerCommonMetaData<T> metaData;
    private T lastKnownValue;
    private long lastUpdateTime;
    private CompletableFuture<T> cachedFuture;

    protected CrossServerCommonMetaDataValue(MetaDataUser user, CrossServerCommonMetaData<T> metaData) {
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
            this.lastUpdateTime = System.currentTimeMillis();
            return it;
        });
    }

    @Override
    public CompletableFuture<T> lastKnownValue() {
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
     * 多服务器强制更新本来就不安全，应该用更安全的add take
     *
     * @param value 值
     * @param markForSave 是否保存与同步
     */
    @Override
    public CompletableFuture<Boolean> update(T value, long time, boolean markForSave) {
        if (!updateLastKnownValue(value, time)) {
            return CompletableFuture.completedFuture(false);
        }
        if (markForSave) {
            return this.user.repository()
                    .update(Map.of(collection(), List.of(new FriendlyData<>(this.metaData.id, this.metaData.dataType, value))))
                    .handle((__, t) -> {
                        if (t == null) {
                            this.user.manager().messageBroker().publishOneWay(new BroadcastCrossServerMetadataValueMessage(this.user.uuid(), this.metaData.id, this.metaData.dataType.encode(value), time), "");
                            return true;
                        }
                        LOGGER.error("Error updating cross server double metadata {} for player {}", this.metaData.id, this.user.uuid(), t);
                        return false;
                    });
        }
        return CompletableFuture.completedFuture(true);
    }

    private boolean updateLastKnownValue(T value, long time) {
        if (this.lastUpdateTime < time) {
            this.lastKnownValue = value;
            this.lastUpdateTime = time;
            return true;
        } else {
            return false;
        }
    }
}
