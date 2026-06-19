package net.momirealms.sparrow.metadata;

import io.netty.buffer.ByteBuf;
import net.momirealms.sparrow.redis.messagebroker.util.ByteBufHelper;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.UUID;

final class Util {
    private Util() {}

    // 编码：Instant -> byte[]
    public static byte[] encodeInstant(Instant instant) {
        long seconds = instant.getEpochSecond();
        int nanos = instant.getNano();

        ByteBuffer buffer = ByteBuffer.allocate(12); // 8字节秒 + 4字节纳秒
        buffer.putLong(seconds);
        buffer.putInt(nanos);

        return buffer.array();
    }

    // 解码：byte[] -> Instant
    public static Instant decodeInstant(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        long seconds = buffer.getLong();
        int nanos = buffer.getInt();

        return Instant.ofEpochSecond(seconds, nanos);
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUUID(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }
}
