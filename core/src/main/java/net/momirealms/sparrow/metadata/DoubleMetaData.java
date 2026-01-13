package net.momirealms.sparrow.metadata;

public class DoubleMetaData extends AbstractMetaData<Double, DoubleMetaDataValue> {

    protected DoubleMetaData(String id, String collection) {
        super(id, collection, false, false, DataType.DOUBLE);
    }

    @Override
    public DoubleMetaDataValue createHolder(MetaDataUser user) {
        return new DoubleMetaDataValue(user, this);
    }
}
