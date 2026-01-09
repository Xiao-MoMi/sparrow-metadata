package net.momirealms.sparrow.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrossServerIntMetaDataValue implements CrossServerMetaDataValue<Integer>, NumericMetaDataValue<Integer> {
    protected final MetaDataUser user;
    protected final CrossServerIntMetaData metaData;
    private Integer lastKnownValue;
    private long lastUpdateTime;
    private CompletableFuture<Integer> cachedFuture;

    protected CrossServerIntMetaDataValue(MetaDataUser user, CrossServerIntMetaData metaData) {
        this.user = user;
        this.metaData = metaData;
    }

    @Override
    public MetaData<Integer, ? extends MetaDataValue<Integer>> metadata() {
        return this.metaData;
    }

    @Override
    public MetaDataUser owner() {
        return this.user;
    }

    @Override
    public CompletableFuture<Response<Integer>> add(Number value) {
        int i = value.intValue();
        return this.user.repository()
                .increaseAndGet(this.metaData, i)
                .thenApply(after -> createResponse(i, after));
    }

    @Override
    public CompletableFuture<Response<Integer>> take(Number value, boolean checkBalance) {
        int i = value.intValue();
        return this.user.repository().decreaseAndGet(this.metaData, i, checkBalance)
                .thenApply(after -> {
                    // 余额不足
                    if (after == null) {
                        return Response.failure();
                    }
                    return createResponse(i, after);
                });
    }

    @NotNull
    private Response<Integer> createResponse(Integer value, Integer after) {
        long time = System.currentTimeMillis();
        this.lastUpdateTime = time;
        this.lastKnownValue = after;
        this.user.manager().messageBroker().publishOneWay(new BroadcastCrossServerMetadataValueMessage(this.user.uuid(), this.metaData.id, this.metaData.dataType.encode(after), time), "");
        return Response.success(value, after);
    }

    @Override
    public CompletableFuture<Integer> get() {
        return this.user.repository().get(this.metaData).thenApply(it -> {
            this.lastKnownValue = it;
            this.lastUpdateTime = System.currentTimeMillis();
            return it;
        });
    }

    @Override
    public CompletableFuture<Integer> lastKnownValue() {
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
    public CompletableFuture<Boolean> update(Integer value, long time, boolean markForSave) {
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
                        LOGGER.error("Error updating cross server int metadata {} for player {}", this.metaData.id, this.user.uuid(), t);
                        return false;
                    });
        }
        return CompletableFuture.completedFuture(true);
    }

    private boolean updateLastKnownValue(Integer value, long time) {
        if (this.lastUpdateTime < time) {
            this.lastKnownValue = value;
            this.lastUpdateTime = time;
            return true;
        } else {
            return false;
        }
    }
}
