package net.momirealms.sparrow.metadata;

public class DoubleMetaData extends CommonMetaData<Double> {

    protected DoubleMetaData(String id, String collection) {
        super(id, collection, DataType.DOUBLE);
    }

    @Override
    public DoubleMetaDataValue createHolder(MetaDataUser user) {
        return new DoubleMetaDataValue(user, this);
    }
}
