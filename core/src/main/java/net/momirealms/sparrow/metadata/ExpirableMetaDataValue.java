package net.momirealms.sparrow.metadata;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

public interface ExpirableMetaDataValue<T> extends MetaDataValue<T> {
    String SUFFIX = "__expire__";

    default CompletableFuture<Boolean> update(T value, Instant expiryTime, boolean markForSave) {
        return this.update(value, markForSave).thenCombine(update(expiryTime, markForSave), (b1, b2) -> {
            return b1 || b2;
        });
    }

    CompletableFuture<Boolean> update(Instant time, boolean markForSave);

    default CompletableFuture<Boolean> update(Instant time) {
        return this.update(time, true);
    }

    /**
     * 如果没有超时时间可能为空
     *
     * @return 超时时间
     */
    @Nullable
    Instant expiryTime();
}
