package net.momirealms.sparrow.metadata;

/**
 * 跨服数值类型元数据值的抽象基类，在 {@link AbstractCrossServerMetaDataValue} 基础上
 * 实现 {@link NumericMetaDataValue}，提供 {@link #createResponse} 的通用实现。
 * 子类需覆写 {@link #add} 和 {@link #take} 以匹配具体数值类型的重载分发。
 *
 * @param <T> 数值类型
 */
public abstract class AbstractCrossServerNumericMetaDataValue<T extends Number>
        extends AbstractCrossServerMetaDataValue<T>
        implements NumericMetaDataValue<T> {

    protected AbstractCrossServerNumericMetaDataValue(MetaDataUser user, MetaData<T, ? extends MetaDataValue<T>> metaData) {
        super(user, metaData);
    }

    /**
     * 构造操作结果并发布跨服同步消息。
     */
    protected Response<T> createResponse(T changed, T after) {
        long time = System.nanoTime();
        this.lastUpdateTime = time;
        this.lastKnownValue = after;
        publishUpdate(after, time);
        return Response.success(changed, after);
    }
}
