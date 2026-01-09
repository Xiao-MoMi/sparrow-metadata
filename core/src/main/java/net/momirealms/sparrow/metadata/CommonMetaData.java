package net.momirealms.sparrow.metadata;

public class CommonMetaData<T> extends AbstractMetaData<T, CommonMetaDataValue<T>> {

    protected CommonMetaData(String id, String collection, DataType<T> dataType) {
        super(id, collection, false, false, dataType);
    }

    @Override
    public CommonMetaDataValue<T> createHolder(MetaDataUser user) {
        return new CommonMetaDataValue<>(user, this);
    }
}
