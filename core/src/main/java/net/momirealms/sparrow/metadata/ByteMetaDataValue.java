package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public class ByteMetaDataValue extends CommonMetaDataValue<Byte> implements NumericMetaDataValue<Byte> {

    protected ByteMetaDataValue(MetaDataUser user, ByteMetaData metaData) {
        super(user, metaData);
    }

    @Override
    public CompletableFuture<Response<Byte>> add(Number value) {
        byte i = value.byteValue();
        byte cachedValue = getCachedValue();
        byte result = (byte) (cachedValue + i);
        update(result);
        return CompletableFuture.completedFuture(Response.success(i, result));
    }

    @Override
    public CompletableFuture<Response<Byte>> take(Number value, boolean checkBalance) {
        byte i = value.byteValue();
        byte cachedValue = getCachedValue();
        if (checkBalance && cachedValue < i) {
            return CompletableFuture.completedFuture(Response.failure());
        }
        byte result = (byte) (cachedValue - i);
        update(result);
        return CompletableFuture.completedFuture(Response.success(i, result));
    }
}
