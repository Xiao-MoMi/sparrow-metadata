package net.momirealms.sparrow.metadata;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.function.Function;

public final class DataType<U> {

    public static final DataType<Instant> INSTANT = new DataType<>(
            Instant.class,
            Instant.ofEpochMilli(0),
            Util::encodeInstant,
            Util::decodeInstant,
            obj -> {
                if (obj instanceof Instant n) return n;
                if (obj instanceof Date d) return d.toInstant();
                if (obj instanceof Number n) return Instant.ofEpochMilli(n.longValue());
                if (obj instanceof String s) return Instant.parse(s);
                return null;
            }
    );

    public static final DataType<Integer> INT = new DataType<>(
            Integer.class,
            0,
            val -> ByteBuffer.allocate(Integer.BYTES).putInt(val).array(),
            bytes -> ByteBuffer.wrap(bytes).getInt(),
            obj -> {
                if (obj instanceof Number n) return n.intValue();
                if (obj instanceof String s) return Integer.parseInt(s);
                return null;
            }
    );

    public static final DataType<Long> LONG = new DataType<>(
            Long.class,
            0L,
            val -> ByteBuffer.allocate(Long.BYTES).putLong(val).array(),
            bytes -> ByteBuffer.wrap(bytes).getLong(),
            obj -> {
                if (obj instanceof Number n) return n.longValue();
                if (obj instanceof String s) return Long.parseLong(s);
                return null;
            }
    );

    public static final DataType<Double> DOUBLE = new DataType<>(
            Double.class,
            0.0,
            val -> ByteBuffer.allocate(Double.BYTES).putDouble(val).array(),
            bytes -> ByteBuffer.wrap(bytes).getDouble(),
            obj -> {
                if (obj instanceof Number n) return n.doubleValue();
                if (obj instanceof String s) return Double.parseDouble(s);
                return null;
            }
    );

    public static final DataType<String> STRING = new DataType<>(
            String.class,
            "",
            val -> val.getBytes(StandardCharsets.UTF_8),
            bytes -> new String(bytes, StandardCharsets.UTF_8),
            Object::toString
    );

    public static final DataType<byte[]> BYTE_ARRAY = new DataType<>(
            byte[].class,
            new byte[0],
            val -> val,
            bytes -> bytes,
            obj -> {
                if (obj instanceof byte[] b) return b;
                if (obj instanceof Collection<?> col) {
                    byte[] res = new byte[col.size()];
                    int i = 0;
                    for (Object o : col) res[i++] = ((Number) o).byteValue();
                    return res;
                }
                return null;
            }
    );

    public static final DataType<int[]> INT_ARRAY = new DataType<>(
            int[].class,
            new int[0],
            val -> {
                ByteBuffer buffer = ByteBuffer.allocate(val.length * Integer.BYTES);
                for (int i : val) buffer.putInt(i);
                return buffer.array();
            },
            bytes -> {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                int[] res = new int[bytes.length / Integer.BYTES];
                for (int i = 0; i < res.length; i++) res[i] = buffer.getInt();
                return res;
            },
            obj -> {
                if (obj instanceof int[] ia) return ia;
                if (obj instanceof Collection<?> col) {
                    return col.stream().mapToInt(o -> ((Number) o).intValue()).toArray();
                }
                if (obj instanceof Integer[] ia) {
                    int[] res = new int[ia.length];
                    for (int i = 0; i < ia.length; i++) res[i] = ia[i];
                    return res;
                }
                return null;
            }
    );

    public static final DataType<long[]> LONG_ARRAY = new DataType<>(
            long[].class,
            new long[0],
            val -> {
                ByteBuffer buffer = ByteBuffer.allocate(val.length * Long.BYTES);
                for (long l : val) buffer.putLong(l);
                return buffer.array();
            },
            bytes -> {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                long[] res = new long[bytes.length / Long.BYTES];
                for (int i = 0; i < res.length; i++) res[i] = buffer.getLong();
                return res;
            },
            obj -> {
                if (obj instanceof long[] la) return la;
                if (obj instanceof Collection<?> col) {
                    return col.stream().mapToLong(o -> ((Number) o).longValue()).toArray();
                }
                if (obj instanceof Long[] la) {
                    long[] res = new long[la.length];
                    for (int i = 0; i < la.length; i++) res[i] = la[i];
                    return res;
                }
                return null;
            }
    );

    private final Class<U> type;
    private final Function<byte[], U> decoder;
    private final Function<U, byte[]> encoder;
    private final Function<Object, U> parser;
    private final U defaultValue;

    private DataType(Class<U> type, U defaultValue, Function<U, byte[]> encoder, Function<byte[], U> decoder, Function<Object, U> parser) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.encoder = encoder;
        this.decoder = decoder;
        this.parser = parser;
    }

    public U defaultValue() {
        return this.defaultValue;
    }

    public Class<U> type() {
        return this.type;
    }

    public byte[] encode(U value) {
        return this.encoder.apply(value);
    }

    public U decode(byte[] value) {
        return this.decoder.apply(value);
    }

    public U parse(Object value) {
        return this.parser.apply(value);
    }
}