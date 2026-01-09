package net.momirealms.sparrow.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrossServerDoubleMetaDataValue implements CrossServerMetaDataValue<Double>, NumericMetaDataValue<Double> {
    protected final MetaDataUser user;
    protected final CrossServerDoubleMetaData metaData;
    private Double lastKnownValue;
    private long lastUpdateTime;
    private CompletableFuture<Double> cachedFuture;

    protected CrossServerDoubleMetaDataValue(MetaDataUser user, CrossServerDoubleMetaData metaData) {
        this.user = user;
        this.metaData = metaData;
    }

    @Override
    public MetaData<Double, ? extends MetaDataValue<Double>> metadata() {
        return this.metaData;
    }

    @Override
    public MetaDataUser owner() {
        return this.user;
    }

    @Override
    public CompletableFuture<Response<Double>> add(Number value) {
        double d = value.doubleValue();
        return this.user.repository()
                .increaseAndGet(this.metaData, d)
                .thenApply(after -> createResponse(d, after));
    }

    @Override
    public CompletableFuture<Response<Double>> take(Number value, boolean checkBalance) {
        double d = value.doubleValue();
        return this.user.repository().decreaseAndGet(this.metaData, d, checkBalance)
                .thenApply(after -> {
                    // 余额不足
                    if (after == null) {
                        return Response.failure();
                    }
                    return createResponse(d, after);
                });
    }

    @NotNull
    private Response<Double> createResponse(Double value, Double after) {
        long time = System.currentTimeMillis();
        this.lastUpdateTime = time;
        this.lastKnownValue = after;
        this.user.manager().messageBroker().publishOneWay(new BroadcastCrossServerMetadataValueMessage(this.user.uuid(), this.metaData.id, this.metaData.dataType.encode(after), time), "");
        return Response.success(value, after);
    }

    @Override
    public CompletableFuture<Double> get() {
        return this.user.repository().get(this.metaData).thenApply(it -> {
            this.lastKnownValue = it;
            this.lastUpdateTime = System.currentTimeMillis();
            return it;
        });
    }

    @Override
    public CompletableFuture<Double> lastKnownValue() {
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
    public CompletableFuture<Boolean> update(Double value, long time, boolean markForSave) {
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

    private boolean updateLastKnownValue(Double value, long time) {
        if (this.lastUpdateTime < time) {
            this.lastKnownValue = value;
            this.lastUpdateTime = time;
            return true;
        } else {
            return false;
        }
    }
}
