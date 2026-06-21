package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public class CrossServerIntMetaDataValue extends AbstractCrossServerNumericMetaDataValue<Integer> {

    protected CrossServerIntMetaDataValue(MetaDataUser user, CrossServerIntMetaData metaData) {
        super(user, metaData);
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
                    if (after == null) {
                        return Response.failure();
                    }
                    return createResponse(i, after);
                });
    }
}
