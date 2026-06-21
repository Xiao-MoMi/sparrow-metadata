package net.momirealms.sparrow.metadata;

public class DoubleMetaDataValue extends AbstractNumericMetaDataValue<Double> {

    protected DoubleMetaDataValue(MetaDataUser user, DoubleMetaData metaData) {
        super(user, metaData);
    }

    @Override
    protected Double fromNumber(Number value) {
        return value.doubleValue();
    }

    @Override
    protected Double zero() {
        return 0.0;
    }

    @Override
    protected Double addValues(Double a, Double b) {
        return a + b;
    }

    @Override
    protected Double subtractValues(Double a, Double b) {
        return a - b;
    }

    @Override
    protected boolean lessThan(Double a, Double b) {
        return a < b;
    }
}
