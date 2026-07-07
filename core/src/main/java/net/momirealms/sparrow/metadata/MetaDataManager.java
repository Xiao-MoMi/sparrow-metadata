package net.momirealms.sparrow.metadata;

import io.netty.buffer.ByteBuf;
import net.momirealms.sparrow.redis.messagebroker.MessageBroker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@SuppressWarnings("rawtypes")
public interface MetaDataManager {
    Logger LOGGER = LoggerFactory.getLogger(MetaDataManager.class);

    static MetaDataManager create(PersistentRepository repository, MessageBroker messageBroker) {
        return new MetaDataManagerImpl(repository, messageBroker);
    }

    void registerMetaData(MetaData metaData);

    default void registerMetaData(MetaData... metaData) {
        for (MetaData type : metaData) {
            registerMetaData(type);
        }
    }

    void unregisterMetaData(String key);

    default void unregisterMetaData(String... keys) {
        for (String key : keys) {
            unregisterMetaData(key);
        }
    }

    MetaData[] registeredMetaData();

    void clearMetaData();

    @Nullable
    <T, H extends MetaDataValue<T>> MetaData<T, H> getMetaData(@NotNull String id);

    String[] collections();

    @NotNull
    MetaDataUser getUser(UUID uuid);

    @Nullable MetaDataUser getOptionalUser(UUID uuid);

    @Nullable
    MetaDataUser createOnlineUser(UUID uuid, Supplier<Boolean> onlineIndicator, boolean acquireLock);

    CompletableFuture<Void> removeOnlineUser(UUID uuid, boolean saveDirtyData);

    @NotNull
    PersistentRepository repository();

    MessageBroker<ByteBuf> messageBroker();

    @SuppressWarnings("rawtypes")
    CompletableFuture<List<UUID>> getRankedUsers(MetaData metaData, int from, int to, boolean ascending);
}
