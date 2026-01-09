package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public class IntMetaDataValue extends CommonMetaDataValue<Integer> implements NumericMetaDataValue<Integer> {

    protected IntMetaDataValue(MetaDataUser user, IntMetaData metaData) {
        super(user, metaData);
    }

    @Override
    public CompletableFuture<Response<Integer>> add(Number value) {
        int i = value.intValue();
        int cachedValue = getCachedValue();
        int result = cachedValue + i;
        update(result);
        return CompletableFuture.completedFuture(Response.success(i, result));
    }

    @Override
    public CompletableFuture<Response<Integer>> take(Number value, boolean checkBalance) {
        int i = value.intValue();
        int cachedValue = getCachedValue();
        if (checkBalance && cachedValue < i) {
            return CompletableFuture.completedFuture(Response.failure());
        }
        int result = cachedValue - i;
        update(result);
        return CompletableFuture.completedFuture(Response.success(i, result));
    }
}
