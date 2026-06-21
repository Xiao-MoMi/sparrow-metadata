package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public class CrossServerByteMetaDataValue extends AbstractCrossServerNumericMetaDataValue<Byte> {

    protected CrossServerByteMetaDataValue(MetaDataUser user, CrossServerByteMetaData metaData) {
        super(user, metaData);
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
                    if (after == null) {
                        return Response.failure();
                    }
                    return createResponse(i, after);
                });
    }
}
