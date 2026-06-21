package net.momirealms.sparrow.metadata;

public class IntMetaDataValue extends AbstractNumericMetaDataValue<Integer> {

    protected IntMetaDataValue(MetaDataUser user, IntMetaData metaData) {
        super(user, metaData);
    }

    @Override
    protected Integer fromNumber(Number value) {
        return value.intValue();
    }

    @Override
    protected Integer zero() {
        return 0;
    }

    @Override
    protected Integer addValues(Integer a, Integer b) {
        return a + b;
    }

    @Override
    protected Integer subtractValues(Integer a, Integer b) {
        return a - b;
    }

    @Override
    protected boolean lessThan(Integer a, Integer b) {
        return a < b;
    }
}
