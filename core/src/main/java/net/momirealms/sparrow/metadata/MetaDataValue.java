package net.momirealms.sparrow.metadata;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface MetaDataValue<T> {
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

    /**
     * 强制更新值到存储层，绕过在线状态检查和时间戳竞争。
     * 用于管理指令等需要直接覆盖的场景。
     *
     * @param value 值
     * @return CompletableFuture
     */
    default CompletableFuture<Void> forceUpdate(T value) {
        return this.update(value, true).thenApply(b -> null);
    }

    default String collection() {
        return Optional.ofNullable(metadata().collection()).orElse(owner().repository().defaultCollectionName());
    }
}
