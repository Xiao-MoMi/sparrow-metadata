package net.momirealms.sparrow.metadata;

import org.jetbrains.annotations.NotNull;

public final class MetaDataProvider {
    private static MetaDataManager instance;

    private MetaDataProvider() {}

    public static MetaDataManager get() {
        if (instance == null) {
            throw new IllegalStateException("MetaDataProvider has not been initialized");
        }
        return instance;
    }

    static void set(@NotNull MetaDataManager manager) {
        if (instance != null) {
            throw new IllegalStateException("MetaDataProvider has already been initialized");
        }
        instance = manager;
    }
}
