package net.momirealms.sparrow.metadata;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ByteMetaDataValue extends CommonMetaDataValue<Byte> implements NumericMetaDataValue<Byte> {

    protected ByteMetaDataValue(MetaDataUser user, ByteMetaData metaData) {
        super(user, metaData);
    }

    public CompletableFuture<Boolean> getAsBoolean() {
        return get().thenApply(value -> value == 1);
    }

    public Boolean getCachedValueAsBoolean() {
        return Optional.ofNullable(getCachedValue()).map(it -> it == 1).orElse(null);
    }

    public boolean getCachedValueAsBoolean(boolean defaultValue) {
        return Optional.ofNullable(getCachedValue()).map(it -> it == 1).orElse(defaultValue);
    }

    public boolean getCachedValueAsBoolean(Supplier<Boolean> defaultValue) {
        return Optional.ofNullable(getCachedValue()).map(it -> it == 1).orElseGet(defaultValue);
    }

    public CompletableFuture<Boolean> update(Boolean value) {
        return update(value == null ? null : (value ? (byte) 1 : (byte) 0), true);
    }

    public CompletableFuture<Boolean> update(Boolean value, boolean markForSave) {
        return update(value == null ? null : (value ? (byte) 1 : (byte) 0), markForSave);
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
