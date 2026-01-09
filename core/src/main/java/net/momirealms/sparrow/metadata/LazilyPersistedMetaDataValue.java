package net.momirealms.sparrow.metadata;

import java.util.function.Consumer;

public interface LazilyPersistedMetaDataValue<T> extends MetaDataValue<T> {

    void saveIfDirty(Consumer<FriendlyData<?>> callback);
}