package net.momirealms.sparrow.metadata;

public class CrossServerDoubleMetaData extends AbstractMetaData<Double, CrossServerDoubleMetaDataValue> {

    protected CrossServerDoubleMetaData(String id, String collection) {
        super(id, collection, false, true, DataType.DOUBLE);
    }

    @Override
    public CrossServerDoubleMetaDataValue createHolder(MetaDataUser user) {
        return new CrossServerDoubleMetaDataValue(user, this);
    }
}
