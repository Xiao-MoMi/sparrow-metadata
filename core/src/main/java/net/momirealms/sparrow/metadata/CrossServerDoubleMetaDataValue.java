package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public class CrossServerDoubleMetaDataValue extends AbstractCrossServerNumericMetaDataValue<Double> {

    protected CrossServerDoubleMetaDataValue(MetaDataUser user, CrossServerDoubleMetaData metaData) {
        super(user, metaData);
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
                    if (after == null) {
                        return Response.failure();
                    }
                    return createResponse(d, after);
                });
    }
}
