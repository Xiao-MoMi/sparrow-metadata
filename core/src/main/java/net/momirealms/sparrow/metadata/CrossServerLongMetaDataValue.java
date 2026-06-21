package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public class CrossServerLongMetaDataValue extends AbstractCrossServerNumericMetaDataValue<Long> {

    protected CrossServerLongMetaDataValue(MetaDataUser user, CrossServerLongMetaData metaData) {
        super(user, metaData);
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
                    if (after == null) {
                        return Response.failure();
                    }
                    return createResponse(l, after);
                });
    }
}
