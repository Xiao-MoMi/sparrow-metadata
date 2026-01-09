package net.momirealms.sparrow.metadata;

public record FriendlyData<T>(String storageId, DataType<T> dataType, T data) {

    public byte[] toRedisValue() {
        return this.dataType.encode(this.data);
    }
}
