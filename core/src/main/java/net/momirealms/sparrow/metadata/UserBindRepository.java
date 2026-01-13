package net.momirealms.sparrow.metadata;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class UserBindRepository {
    private final UUID user;
    private final PersistentRepository repository;

    UserBindRepository(UUID user, PersistentRepository repository) {
        this.user = user;
        this.repository = repository;
    }

    public String defaultCollectionName() {
        return this.repository.defaultCollectionName();
    }

    public <T> CompletableFuture<T> get(MetaData<T, ?> key) {
        return this.repository.get(this.user, key);
    }

    public CompletableFuture<Instant> getExpiryTime(String metadataKey) {
        return this.repository.getExpiryTime(this.user, metadataKey);
    }

    public CompletableFuture<Void> update(Map<String, List<FriendlyData<?>>> metadata) {
        return this.repository.update(this.user, metadata);
    }

    public CompletableFuture<Map<String, Object>> getAll(String[] collections) {
        return this.repository.getAll(this.user, collections);
    }

    public CompletableFuture<Byte> increaseAndGet(MetaData<Byte, ?> metaData, byte value) {
        return this.repository.increaseAndGet(this.user, metaData, value);
    }

    public CompletableFuture<Double> increaseAndGet(MetaData<Double, ?> metaData, double value) {
        return this.repository.increaseAndGet(this.user, metaData, value);
    }

    public CompletableFuture<Integer> increaseAndGet(MetaData<Integer, ?> metaData, int value) {
        return this.repository.increaseAndGet(this.user, metaData, value);
    }

    public CompletableFuture<Long> increaseAndGet(MetaData<Long, ?> metaData, long value) {
        return this.repository.increaseAndGet(this.user, metaData, value);
    }

    public CompletableFuture<Byte> decreaseAndGet(MetaData<Byte, ?> metaData, byte value, boolean checkBalance) {
        return this.repository.decreaseAndGet(this.user, metaData, value, checkBalance);
    }

    public CompletableFuture<Double> decreaseAndGet(MetaData<Double, ?> metaData, double value, boolean checkBalance) {
        return this.repository.decreaseAndGet(this.user, metaData, value, checkBalance);
    }

    public CompletableFuture<Integer> decreaseAndGet(MetaData<Integer, ?> metaData, int value, boolean checkBalance) {
        return this.repository.decreaseAndGet(this.user, metaData, value, checkBalance);
    }

    public CompletableFuture<Long> decreaseAndGet(MetaData<Long, ?> metaData, long value, boolean checkBalance) {
        return this.repository.decreaseAndGet(this.user, metaData, value, checkBalance);
    }
}
