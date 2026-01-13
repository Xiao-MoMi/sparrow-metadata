package net.momirealms.sparrow.metadata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface MetaDataValue<T> {
    Logger LOGGER = LoggerFactory.getLogger(MetaDataValue.class);

    MetaDataUser owner();

    MetaData<T, ? extends MetaDataValue<T>> metadata();

    CompletableFuture<T> get();

    default CompletableFuture<T> getOrDefault(T defaultValue) {
        return get().thenApply(value -> value == null ? defaultValue : value);
    }

    default CompletableFuture<T> getOrDefault(Supplier<T> defaultValue) {
        return get().thenApply(value -> value == null ? defaultValue.get() : value);
    }

    default CompletableFuture<Boolean> update(T value) {
        return this.update(value, true);
    }

    CompletableFuture<Boolean> update(T value, boolean markForSave);

    default String collection() {
        return Optional.ofNullable(metadata().collection()).orElse(owner().repository().defaultCollectionName());
    }
}
