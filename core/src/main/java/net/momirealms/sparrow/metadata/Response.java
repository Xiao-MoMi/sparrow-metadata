package net.momirealms.sparrow.metadata;

public final class Response<T extends Number> {
    private final boolean success;
    private final T changed;
    private final T after;

    private Response(boolean success, T changed, T after) {
        this.success = success;
        this.changed = changed;
        this.after = after;
    }

    public static <T extends Number> Response<T> success(T changed, T after) {
        return new Response<>(true, changed, after);
    }

    public static <T extends Number> Response<T> failure() {
        return new Response<>(false, null, null);
    }

    public boolean success() {
        return success;
    }

    public T changed() {
        return changed;
    }

    public T after() {
        return after;
    }
}
