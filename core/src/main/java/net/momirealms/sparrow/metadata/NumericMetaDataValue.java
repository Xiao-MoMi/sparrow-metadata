package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public interface NumericMetaDataValue<T extends Number> extends MetaDataValue<T> {

    CompletableFuture<Response<T>> add(Number value);

    CompletableFuture<Response<T>> take(Number value, boolean checkBalance);
}
