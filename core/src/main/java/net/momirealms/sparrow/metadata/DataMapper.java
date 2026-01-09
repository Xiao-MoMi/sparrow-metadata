package net.momirealms.sparrow.metadata;

import java.util.function.Function;

public interface DataMapper<T, U> {

    U serialize(final T t);

    T deserialize(final U u);

    static <U> DataMapper<U, U> createIdentityMapper() {
        return new DataMapper<>() {
            @Override
            public U serialize(U u) {
                return u;
            }

            @Override
            public U deserialize(U u) {
                return u;
            }
        };
    }

    static <T, U> DataMapper<T, U> createMapper(Function<T, U> serializer, Function<U, T> deserializer) {
        return new DataMapper<>() {
            @Override
            public U serialize(T t) {
                return serializer.apply(t);
            }

            @Override
            public T deserialize(U u) {
                return deserializer.apply(u);
            }
        };
    }
}