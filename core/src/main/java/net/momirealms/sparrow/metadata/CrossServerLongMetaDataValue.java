package net.momirealms.sparrow.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrossServerLongMetaDataValue implements CrossServerMetaDataValue<Long>, NumericMetaDataValue<Long> {
    protected final MetaDataUser user;
    protected final CrossServerLongMetaData metaData;
    private Long lastKnownValue;
    private long lastUpdateTime;
    private CompletableFuture<Long> cachedFuture;

    protected CrossServerLongMetaDataValue(MetaDataUser user, CrossServerLongMetaData metaData) {
        this.user = user;
        this.metaData = metaData;
    }

    @Override
    public MetaData<Long, ? extends MetaDataValue<Long>> metadata() {
        return this.metaData;
    }

    @Override
    public MetaDataUser owner() {
        return this.user;
    }

    @Override
    public CompletableFuture<Response<Long>> add(Number value) {
        long l = value.longValue();
        return this.user.repository()
                .increaseAndGet(this.metaData, l)
                .thenApply(after -> createResponse(l, after));
    }

    @Override
    public CompletableFuture<Response<Long>> take(Number value, boolean checkBalance) {
        long l = value.longValue();
        return this.user.repository().decreaseAndGet(this.metaData, l, checkBalance)
                .thenApply(after -> {
                    // 余额不足
                    if (after == null) {
                        return Response.failure();
                    }
                    return createResponse(l, after);
                });
    }

    @NotNull
    private Response<Long> createResponse(Long value, Long after) {
        long time = System.nanoTime();
        this.lastUpdateTime = time;
        this.lastKnownValue = after;
        this.user.manager().messageBroker().publishOneWay(new BroadcastCrossServerMetadataValueMessage(this.user.uuid(), this.metaData.id, this.metaData.dataType.encode(after), time), "");
        return Response.success(value, after);
    }

    @Override
    public CompletableFuture<Long> get() {
        return this.user.repository().get(this.metaData).thenApply(it -> {
            this.lastKnownValue = it;
            this.lastUpdateTime = System.nanoTime();
            return it;
        });
    }

    @Override
    public CompletableFuture<Long> lastKnownValue() {
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
    public CompletableFuture<Boolean> update(Long value, long time, boolean markForSave) {
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
                        LOGGER.error("Error updating cross server long metadata {} for player {}", this.metaData.id, this.user.uuid(), t);
                        return false;
                    });
        }
        return CompletableFuture.completedFuture(true);
    }

    private boolean updateLastKnownValue(Long value, long time) {
        if (this.lastUpdateTime <= time) {
            this.lastKnownValue = value;
            this.lastUpdateTime = time;
            return true;
        } else {
            return false;
        }
    }
}
