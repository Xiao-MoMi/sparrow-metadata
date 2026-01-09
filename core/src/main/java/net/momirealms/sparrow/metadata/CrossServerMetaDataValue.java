package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public interface CrossServerMetaDataValue<T> extends MetaDataValue<T> {

    CompletableFuture<T> lastKnownValue();

    default CompletableFuture<Boolean> update(T value, boolean markForSave) {
        return update(value, System.currentTimeMillis(), markForSave);
    }

    CompletableFuture<Boolean> update(T value, long time, boolean markForSave);
}
