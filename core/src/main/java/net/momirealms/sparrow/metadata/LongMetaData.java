package net.momirealms.sparrow.metadata;

public class LongMetaData extends AbstractMetaData<Long, LongMetaDataValue> {

    protected LongMetaData(String id, String collection) {
        super(id, collection, false, false, DataType.LONG);
    }

    @Override
    public LongMetaDataValue createHolder(MetaDataUser user) {
        return new LongMetaDataValue(user, this);
    }
}
