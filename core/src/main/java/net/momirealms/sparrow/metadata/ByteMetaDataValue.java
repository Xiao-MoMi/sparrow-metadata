package net.momirealms.sparrow.metadata;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ByteMetaDataValue extends AbstractNumericMetaDataValue<Byte> {

    protected ByteMetaDataValue(MetaDataUser user, ByteMetaData metaData) {
        super(user, metaData);
    }

    @Override
    protected Byte fromNumber(Number value) {
        return value.byteValue();
    }

    @Override
    protected Byte zero() {
        return (byte) 0;
    }

    @Override
    protected Byte addValues(Byte a, Byte b) {
        return (byte) (a + b);
    }

    @Override
    protected Byte subtractValues(Byte a, Byte b) {
        return (byte) (a - b);
    }

    @Override
    protected boolean lessThan(Byte a, Byte b) {
        return a < b;
    }

    public CompletableFuture<Boolean> getAsBoolean() {
        return get().thenApply(value -> value != null && value == 1);
    }

    public Boolean getCachedValueAsBoolean() {
        return Optional.ofNullable(getCachedValue()).map(it -> it == 1).orElse(null);
    }

    public boolean getCachedValueAsBooleanOrDefault(boolean defaultValue) {
        return Optional.ofNullable(getCachedValue()).map(it -> it == 1).orElse(defaultValue);
    }

    public boolean getCachedValueAsBooleanOrDefault(Supplier<Boolean> defaultValue) {
        return Optional.ofNullable(getCachedValue()).map(it -> it == 1).orElseGet(defaultValue);
    }

    public CompletableFuture<Boolean> getAsBooleanOrDefault(boolean defaultValue) {
        return get().thenApply(value -> value == null ? defaultValue : value != 0);
    }

    public CompletableFuture<Boolean> getAsBooleanOrDefault(Supplier<Boolean> defaultValue) {
        return get().thenApply(value -> value == null ? defaultValue.get() : value != 0);
    }

    public CompletableFuture<Boolean> update(Boolean value) {
        return update(value == null ? null : (value ? (byte) 1 : (byte) 0), true);
    }

    public CompletableFuture<Boolean> update(Boolean value, boolean markForSave) {
        return update(value == null ? null : (value ? (byte) 1 : (byte) 0), markForSave);
    }
}
