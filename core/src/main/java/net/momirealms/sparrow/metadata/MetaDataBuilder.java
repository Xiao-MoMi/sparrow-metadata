package net.momirealms.sparrow.metadata;

public class MetaDataBuilder {
    private final String id;
    private String collection;

    protected MetaDataBuilder(String id) {
        this.id = id;
    }

    public MetaDataBuilder collection(String collection) {
        this.collection = collection;
        return this;
    }

    public ByteBuilder byteType() {return new ByteBuilder(this.id, this.collection);}

    public IntBuilder intType() {
        return new IntBuilder(this.id, this.collection);
    }

    public DoubleBuilder doubleType() {
        return new DoubleBuilder(this.id, this.collection);
    }

    public LongBuilder longType() {
        return new LongBuilder(this.id, this.collection);
    }

    public CommonBuilder<String> stringType() {
        return new CommonBuilder<>(this.id, DataType.STRING, this.collection);
    }

    public CommonBuilder<byte[]> byteArrayType() {
        return new CommonBuilder<>(this.id, DataType.BYTE_ARRAY, this.collection);
    }

    public CommonBuilder<int[]> intArrayType() {
        return new CommonBuilder<>(this.id, DataType.INT_ARRAY, this.collection);
    }

    public CommonBuilder<long[]> longArrayType() {
        return new CommonBuilder<>(this.id, DataType.LONG_ARRAY, this.collection);
    }

    public static class CommonBuilder<T> {
        private final DataType<T> dataType;
        private final String id;
        private String collection;

        private CommonBuilder(String id,
                             DataType<T> dataType,
                             String collection) {
            this.dataType = dataType;
            this.id = id;
            this.collection = collection;
        }

        public CommonBuilder<T> collection(String collection) {
            this.collection = collection;
            return this;
        }

        public CrossServerCommonBuilder<T> crossServerSync() {
            return new CrossServerCommonBuilder<>(this.id, this.dataType, this.collection);
        }

        public ExpirableCommonBuilder<T> expirable() {
            return new ExpirableCommonBuilder<>(this.id, this.dataType, this.collection);
        }

        public CommonMetaData<T> build() {
            return new CommonMetaData<>(this.id, this.collection, this.dataType);
        }
    }

    public static class DoubleBuilder {
        private final String id;
        private String collection;

        private DoubleBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public DoubleBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public ExpirableCommonBuilder<Double> expirable() {
            return new ExpirableCommonBuilder<>(this.id, DataType.DOUBLE, this.collection);
        }

        public CrossServerDoubleBuilder crossServer() {
            return new CrossServerDoubleBuilder(this.id, this.collection);
        }

        public DoubleMetaData build() {
            return new DoubleMetaData(this.id, this.collection);
        }
    }

    public static class CrossServerDoubleBuilder {
        private final String id;
        private String collection;

        public CrossServerDoubleBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public CrossServerDoubleBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public CrossServerDoubleMetaData build() {
            return new CrossServerDoubleMetaData(this.id, this.collection);
        }
    }

    public static class ByteBuilder {
        private final String id;
        private String collection;

        private ByteBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public ByteBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public ExpirableCommonBuilder<Byte> expirable() {
            return new ExpirableCommonBuilder<>(this.id, DataType.BYTE, this.collection);
        }

        public CrossServerByteBuilder crossServer() {
            return new CrossServerByteBuilder(this.id, this.collection);
        }

        public ByteMetaData build() {
            return new ByteMetaData(this.id, this.collection);
        }
    }

    public static class CrossServerByteBuilder {
        private final String id;
        private String collection;

        public CrossServerByteBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public CrossServerByteBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public CrossServerByteMetaData build() {
            return new CrossServerByteMetaData(this.id, this.collection);
        }
    }

    public static class IntBuilder {
        private final String id;
        private String collection;

        private IntBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public IntBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public ExpirableCommonBuilder<Integer> expirable() {
            return new ExpirableCommonBuilder<>(this.id, DataType.INT, this.collection);
        }

        public CrossServerIntBuilder crossServer() {
            return new CrossServerIntBuilder(this.id, this.collection);
        }

        public IntMetaData build() {
            return new IntMetaData(this.id, this.collection);
        }
    }

    public static class CrossServerIntBuilder {
        private final String id;
        private String collection;

        public CrossServerIntBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public CrossServerIntBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public CrossServerIntMetaData build() {
            return new CrossServerIntMetaData(this.id, this.collection);
        }
    }

    public static class LongBuilder {
        private final String id;
        private String collection;

        private LongBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public LongBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public ExpirableCommonBuilder<Long> expirable() {
            return new ExpirableCommonBuilder<>(this.id, DataType.LONG, this.collection);
        }

        public CrossServerLongBuilder crossServer() {
            return new CrossServerLongBuilder(this.id, this.collection);
        }

        public LongMetaData build() {
            return new LongMetaData(this.id, this.collection);
        }
    }

    public static class CrossServerLongBuilder {
        private final String id;
        private String collection;

        public CrossServerLongBuilder(String id, String collection) {
            this.id = id;
            this.collection = collection;
        }

        public CrossServerLongBuilder collection(String collection) {
            this.collection = collection;
            return this;
        }

        public CrossServerLongMetaData build() {
            return new CrossServerLongMetaData(this.id, this.collection);
        }
    }

    public static class ExpirableCommonBuilder<T> {
        private final DataType<T> dataType;
        private final String id;
        private String collection;

        private ExpirableCommonBuilder(String id, DataType<T> dataType, String collection) {
            this.id = id;
            this.collection = collection;
            this.dataType = dataType;
        }

        public ExpirableCommonBuilder<T> collection(String collection) {
            this.collection = collection;
            return this;
        }

        public ExpirableCommonMetaData<T> build() {
            return new ExpirableCommonMetaData<>(this.id, this.collection, this.dataType);
        }
    }

    public static class CrossServerCommonBuilder<T> {
        private final DataType<T> dataType;
        private final String id;
        private String collection;

        private CrossServerCommonBuilder(String id, DataType<T> dataType, String collection) {
            this.id = id;
            this.collection = collection;
            this.dataType = dataType;
        }

        public CrossServerCommonBuilder<T> collection(String collection) {
            this.collection = collection;
            return this;
        }

        public CrossServerCommonMetaData<T> build() {
            return new CrossServerCommonMetaData<>(this.id, this.collection, this.dataType);
        }
    }
}
