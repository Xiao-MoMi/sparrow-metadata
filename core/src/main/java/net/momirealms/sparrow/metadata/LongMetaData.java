package net.momirealms.sparrow.metadata;

public class LongMetaData extends CommonMetaData<Long> {

    protected LongMetaData(String id, String collection) {
        super(id, collection, DataType.LONG);
    }

    @Override
    public LongMetaDataValue createHolder(MetaDataUser user) {
        return new LongMetaDataValue(user, this);
    }
}
