package net.momirealms.sparrow.metadata;

public class ExpirableCommonMetaData<T> extends AbstractMetaData<T, ExpirableCommonMetaDataValue<T>> {

    protected ExpirableCommonMetaData(String id, String collection, DataType<T> dataType) {
        super(id, collection, true, false, dataType);
    }

    @Override
    public ExpirableCommonMetaDataValue<T> createHolder(MetaDataUser user) {
        return new ExpirableCommonMetaDataValue<>(user, this);
    }
}
