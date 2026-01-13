package net.momirealms.sparrow.metadata;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class DoubleMetaDataValue extends CommonMetaDataValue<Double> implements NumericMetaDataValue<Double> {

    protected DoubleMetaDataValue(MetaDataUser user, DoubleMetaData metaData) {
        super(user, metaData);
    }

    @Override
    public CompletableFuture<Response<Double>> add(Number value) {
        double d = value.doubleValue();
        double cachedValue = Optional.ofNullable(getCachedValue()).orElse(0.0);
        double result = cachedValue + d;
        update(result);
        return CompletableFuture.completedFuture(Response.success(d, result));
    }

    @Override
    public CompletableFuture<Response<Double>> take(Number value, boolean checkBalance) {
        double d = value.doubleValue();
        double cachedValue = Optional.ofNullable(getCachedValue()).orElse(0.0);
        if (checkBalance && d > cachedValue) {
            return CompletableFuture.completedFuture(Response.failure());
        }
        double result = cachedValue - d;
        update(result);
        return CompletableFuture.completedFuture(Response.success(d, result));
    }
}
