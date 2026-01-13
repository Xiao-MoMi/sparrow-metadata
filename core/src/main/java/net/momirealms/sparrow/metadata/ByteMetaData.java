package net.momirealms.sparrow.metadata;

public class ByteMetaData extends CommonMetaData<Byte> {

    protected ByteMetaData(String id, String collection) {
        super(id, collection, DataType.BYTE);
    }

    @Override
    public ByteMetaDataValue createHolder(MetaDataUser user) {
        return new ByteMetaDataValue(user, this);
    }
}
