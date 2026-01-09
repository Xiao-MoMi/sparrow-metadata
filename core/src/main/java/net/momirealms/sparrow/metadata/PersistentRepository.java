package net.momirealms.sparrow.metadata;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface PersistentRepository {

    String defaultCollectionName();

    <T> CompletableFuture<T> get(UUID uuid, MetaData<T, ?> key);

    CompletableFuture<Instant> getExpiryTime(UUID uuid, String metadataKey);

    CompletableFuture<Void> update(UUID uuid, Map<String, List<FriendlyData<?>>> metadataList);

    CompletableFuture<Map<String, Object>> getAll(UUID uuid, String[] collections);

    CompletableFuture<Double> increaseAndGet(UUID uuid, MetaData<Double, ?> metaData, double value);

    CompletableFuture<Integer> increaseAndGet(UUID uuid, MetaData<Integer, ?> metaData, int value);

    CompletableFuture<Long> increaseAndGet(UUID uuid, MetaData<Long, ?> metaData, long value);

    CompletableFuture<Double> decreaseAndGet(UUID uuid, MetaData<Double, ?> metaData, double value, boolean checkBalance);

    CompletableFuture<Integer> decreaseAndGet(UUID uuid, MetaData<Integer, ?> metaData, int value, boolean checkBalance);

    CompletableFuture<Long> decreaseAndGet(UUID uuid, MetaData<Long, ?> metaData, long value, boolean checkBalance);

    CompletableFuture<Void> lock(UUID uuid);

    CompletableFuture<Void> unlock(UUID uuid);

    boolean acquireLock(UUID uuid, Supplier<Boolean> onlineIndicator);
}
