package net.momirealms.sparrow.metadata;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MetaDataUser {

    boolean isOnline();

    void setOnline(boolean online);

    boolean loaded();

    default boolean isLoading() {
        return this.isOnline() && !this.loaded();
    }

    MetaDataManager manager();

    UUID uuid();

    UserBindRepository repository();

    void onTimer();

    CompletableFuture<Void> saveDirty();

    CompletableFuture<Void> loadAll();

    <T, H extends MetaDataValue<T>> H getOrCreateValue(MetaData<T, H> metaData);

    <T, H extends MetaDataValue<T>> H getValue(MetaData<T, H> metaData);
}
