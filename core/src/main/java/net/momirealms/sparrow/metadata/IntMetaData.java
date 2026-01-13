package net.momirealms.sparrow.metadata;

public class IntMetaData extends AbstractMetaData<Integer, IntMetaDataValue> {

    protected IntMetaData(String id, String collection) {
        super(id, collection, false, false, DataType.INT);
    }

    @Override
    public IntMetaDataValue createHolder(MetaDataUser user) {
        return new IntMetaDataValue(user, this);
    }
}
