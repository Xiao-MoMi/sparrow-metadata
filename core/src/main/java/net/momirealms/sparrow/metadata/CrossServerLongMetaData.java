package net.momirealms.sparrow.metadata;

public class CrossServerLongMetaData extends AbstractMetaData<Long, CrossServerLongMetaDataValue> {

    protected CrossServerLongMetaData(String id, String collection) {
        super(id, collection, false, true, DataType.LONG);
    }

    @Override
    public CrossServerLongMetaDataValue createHolder(MetaDataUser user) {
        return new CrossServerLongMetaDataValue(user, this);
    }
}
