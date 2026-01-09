package net.momirealms.sparrow.metadata;

import java.nio.ByteBuffer;
import java.time.Instant;

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
}
