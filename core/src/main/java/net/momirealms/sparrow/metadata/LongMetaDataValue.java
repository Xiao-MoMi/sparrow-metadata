package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public class LongMetaDataValue extends CommonMetaDataValue<Long> implements NumericMetaDataValue<Long> {

    protected LongMetaDataValue(MetaDataUser user, LongMetaData metaData) {
        super(user, metaData);
    }

    @Override
    public CompletableFuture<Response<Long>> add(Number value) {
        long l = value.longValue();
        long cachedValue = getCachedValue();
        long result = cachedValue + l;
        update(result);
        return CompletableFuture.completedFuture(Response.success(l, result));
    }

    @Override
    public CompletableFuture<Response<Long>> take(Number value, boolean checkBalance) {
        long l = value.longValue();
        long cachedValue = getCachedValue();
        if (checkBalance && l > cachedValue) {
            return CompletableFuture.completedFuture(Response.failure());
        }
        long result = cachedValue - l;
        update(result);
        return CompletableFuture.completedFuture(Response.success(l, result));
    }
}
