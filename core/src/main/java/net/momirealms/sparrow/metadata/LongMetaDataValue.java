package net.momirealms.sparrow.metadata;

public class LongMetaDataValue extends AbstractNumericMetaDataValue<Long> {

    protected LongMetaDataValue(MetaDataUser user, LongMetaData metaData) {
        super(user, metaData);
    }

    @Override
    protected Long fromNumber(Number value) {
        return value.longValue();
    }

    @Override
    protected Long zero() {
        return 0L;
    }

    @Override
    protected Long addValues(Long a, Long b) {
        return a + b;
    }

    @Override
    protected Long subtractValues(Long a, Long b) {
        return a - b;
    }

    @Override
    protected boolean lessThan(Long a, Long b) {
        return a < b;
    }
}
