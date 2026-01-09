package net.momirealms.sparrow.metadata;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MetaData<T, H extends MetaDataValue<T>> {
    Logger LOGGER = LoggerFactory.getLogger(MetaData.class);

    static MetaDataBuilder builder(String id) {
        return new MetaDataBuilder(id);
    }

    String id();

    @Nullable
    String collection();

    boolean expirable();

    boolean crossServerSync();

    H createHolder(MetaDataUser user);

    DataType<T> dataType();
}
