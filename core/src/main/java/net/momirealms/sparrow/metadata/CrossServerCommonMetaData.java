package net.momirealms.sparrow.metadata;

public class CrossServerCommonMetaData<T> extends AbstractMetaData<T, CrossServerCommonMetaDataValue<T>> {

    protected CrossServerCommonMetaData(String id, String collection, DataType<T> dataType) {
        super(id, collection, false, true, dataType);
    }

    @Override
    public CrossServerCommonMetaDataValue<T> createHolder(MetaDataUser user) {
        return new CrossServerCommonMetaDataValue<>(user, this);
    }
}
