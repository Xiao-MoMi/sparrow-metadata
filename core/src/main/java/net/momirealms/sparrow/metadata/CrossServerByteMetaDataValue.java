package net.momirealms.sparrow.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrossServerByteMetaDataValue implements CrossServerMetaDataValue<Byte>, NumericMetaDataValue<Byte> {
    protected final MetaDataUser user;
    protected final CrossServerByteMetaData metaData;
    private byte lastKnownValue;
    private long lastUpdateTime;
    private CompletableFuture<Byte> cachedFuture;

    protected CrossServerByteMetaDataValue(MetaDataUser user, CrossServerByteMetaData metaData) {
        this.user = user;
        this.metaData = metaData;
    }

    @Override
    public MetaData<Byte, ? extends MetaDataValue<Byte>> metadata() {
        return this.metaData;
    }

    @Override
    public MetaDataUser owner() {
        return this.user;
    }

    @Override
    public CompletableFuture<Response<Byte>> add(Number value) {
        byte i = value.byteValue();
        return this.user.repository()
                .increaseAndGet(this.metaData, i)
                .thenApply(after -> createResponse(i, after));
    }

    @Override
    public CompletableFuture<Response<Byte>> take(Number value, boolean checkBalance) {
        byte i = value.byteValue();
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
    private Response<Byte> createResponse(Byte value, Byte after) {
        long time = System.nanoTime();
        this.lastUpdateTime = time;
        this.lastKnownValue = after;
        this.user.manager().messageBroker().publishOneWay(new BroadcastCrossServerMetadataValueMessage(this.user.uuid(), this.metaData.id, this.metaData.dataType.encode(after), time), "");
        return Response.success(value, after);
    }

    @Override
    public CompletableFuture<Byte> get() {
        return this.user.repository().get(this.metaData).thenApply(it -> {
            this.lastKnownValue = it;
            this.lastUpdateTime = System.nanoTime();
            return it;
        });
    }

    @Override
    public CompletableFuture<Byte> lastKnownValue() {
        if (this.lastUpdateTime != (byte) 0) {
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
    public CompletableFuture<Boolean> update(Byte value, long time, boolean markForSave) {
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

    private boolean updateLastKnownValue(Byte value, long time) {
        if (this.lastUpdateTime < time) {
            this.lastKnownValue = value;
            this.lastUpdateTime = time;
            return true;
        } else {
            return false;
        }
    }
}
