package net.momirealms.sparrow.metadata;

public class IntMetaData extends CommonMetaData<Integer> {

    protected IntMetaData(String id, String collection) {
        super(id, collection, DataType.INT);
    }

    @Override
    public IntMetaDataValue createHolder(MetaDataUser user) {
        return new IntMetaDataValue(user, this);
    }
}
