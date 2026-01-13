package net.momirealms.sparrow.metadata;

public class ByteMetaData extends AbstractMetaData<Byte, ByteMetaDataValue>  {

    protected ByteMetaData(String id, String collection) {
        super(id, collection, false, false, DataType.BYTE);
    }

    @Override
    public ByteMetaDataValue createHolder(MetaDataUser user) {
        return new ByteMetaDataValue(user, this);
    }
}
