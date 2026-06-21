package net.momirealms.sparrow.metadata;

import java.util.concurrent.CompletableFuture;

public interface CrossServerMetaDataValue<T> extends MetaDataValue<T> {

    CompletableFuture<T> lastKnownValue();

    default CompletableFuture<Boolean> update(T value, boolean markForSave) {
        return update(value, System.nanoTime(), markForSave);
    }

    CompletableFuture<Boolean> update(T value, long time, boolean markForSave);

    /**
     * 强制更新跨服元数据，使用 {@link Long#MAX_VALUE} 作为时间戳以绕过
     * last-write-wins 检查。用于管理指令等需要直接覆盖的场景。
     */
    @Override
    default CompletableFuture<Void> forceUpdate(T value) {
        return this.update(value, Long.MAX_VALUE, true).thenApply(b -> null);
    }
}
