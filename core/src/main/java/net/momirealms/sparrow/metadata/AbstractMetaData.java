package net.momirealms.sparrow.metadata;

public abstract class AbstractMetaData<T, H extends MetaDataValue<T>> implements MetaData<T, H> {
    protected final String id;
    protected final boolean expirable;
    protected final boolean crossServerSync;
    protected final DataType<T> dataType;
    protected final String collection;

    protected AbstractMetaData(String id,
                            String collection,
                            boolean expirable,
                            boolean crossServerSync,
                            DataType<T> dataType) {
        this.id = id;
        this.expirable = expirable;
        this.crossServerSync = crossServerSync;
        this.dataType = dataType;
        this.collection = collection;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public boolean expirable() {
        return this.expirable;
    }

    @Override
    public boolean crossServerSync() {
        return this.crossServerSync;
    }

    @Override
    public DataType<T> dataType() {
        return this.dataType;
    }

    @Override
    public String collection() {
        return this.collection;
    }
}
