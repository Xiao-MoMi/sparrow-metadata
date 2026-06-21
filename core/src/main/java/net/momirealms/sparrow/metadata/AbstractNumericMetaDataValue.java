package net.momirealms.sparrow.metadata;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 单服数值类型元数据值的抽象基类，在 {@link CommonMetaDataValue} 基础上
 * 实现 {@link NumericMetaDataValue}，通过模板方法将 add/take 的
 * read-modify-write 操作放入锁内，消除 lost-update 竞态条件。
 *
 * @param <T> 数值类型
 */
public abstract class AbstractNumericMetaDataValue<T extends Number>
        extends CommonMetaDataValue<T>
        implements NumericMetaDataValue<T> {

    protected AbstractNumericMetaDataValue(MetaDataUser user, MetaData<T, ? extends MetaDataValue<T>> metaData) {
        super(user, metaData);
    }

    @Override
    public CompletableFuture<Response<T>> add(Number value) {
        T amount = fromNumber(value);
        this.lock.lock();
        try {
            T current = Optional.ofNullable(this.cachedValue).orElse(zero());
            T result = addValues(current, amount);
            this.cachedValue = result;
            this.markedForSave = true;
            return CompletableFuture.completedFuture(Response.success(amount, result));
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public CompletableFuture<Response<T>> take(Number value, boolean checkBalance) {
        T amount = fromNumber(value);
        this.lock.lock();
        try {
            T current = Optional.ofNullable(this.cachedValue).orElse(zero());
            if (checkBalance && lessThan(current, amount)) {
                return CompletableFuture.completedFuture(Response.failure());
            }
            T result = subtractValues(current, amount);
            this.cachedValue = result;
            this.markedForSave = true;
            return CompletableFuture.completedFuture(Response.success(amount, result));
        } finally {
            this.lock.unlock();
        }
    }

    /** 将 Number 转为具体数值类型 */
    protected abstract T fromNumber(Number value);

    /** 返回零值 */
    protected abstract T zero();

    /** 加法 */
    protected abstract T addValues(T a, T b);

    /** 减法 */
    protected abstract T subtractValues(T a, T b);

    /** 小于比较 */
    protected abstract boolean lessThan(T a, T b);
}
