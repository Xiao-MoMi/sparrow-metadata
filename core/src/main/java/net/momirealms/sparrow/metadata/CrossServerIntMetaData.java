package net.momirealms.sparrow.metadata;

public class CrossServerIntMetaData extends AbstractMetaData<Integer, CrossServerIntMetaDataValue> {

    protected CrossServerIntMetaData(String id, String collection) {
        super(id, collection, false, true, DataType.INT);
    }

    @Override
    public CrossServerIntMetaDataValue createHolder(MetaDataUser user) {
        return new CrossServerIntMetaDataValue(user, this);
    }
}
