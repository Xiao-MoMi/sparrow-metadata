package net.momirealms.sparrow.metadata;

public final class MetaDataConstants {
    public static final ExpirableCommonMetaData<Integer> TEST_INT = MetaData.builder("test_int11")
            .intType()
            .expirable()
            .collection("sb")
            .build();
    public static final CommonMetaData<String> TEST_STR = MetaData.builder("test_str")
            .stringType()
            .build();
    public static final CrossServerDoubleMetaData DOUBLE = MetaData.builder("double")
            .doubleType()
            .crossServer()
            .build();
}
