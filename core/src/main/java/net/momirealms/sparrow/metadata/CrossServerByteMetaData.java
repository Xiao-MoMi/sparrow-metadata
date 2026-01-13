package net.momirealms.sparrow.metadata;

public class CrossServerByteMetaData extends AbstractMetaData<Byte, CrossServerByteMetaDataValue> {

    protected CrossServerByteMetaData(String id, String collection) {
        super(id, collection, false, true, DataType.BYTE);
    }

    @Override
    public CrossServerByteMetaDataValue createHolder(MetaDataUser user) {
        return new CrossServerByteMetaDataValue(user, this);
    }
}
