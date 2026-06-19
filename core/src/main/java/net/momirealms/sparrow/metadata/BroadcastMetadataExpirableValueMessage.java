package net.momirealms.sparrow.metadata;

import io.netty.buffer.ByteBuf;
import net.momirealms.sparrow.redis.messagebroker.MessageIdentifier;
import net.momirealms.sparrow.redis.messagebroker.codec.MessageCodec;
import net.momirealms.sparrow.redis.messagebroker.message.OneWayMessage;
import net.momirealms.sparrow.redis.messagebroker.util.ByteBufHelper;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class BroadcastMetadataExpirableValueMessage extends OneWayMessage<ByteBuf> {
    static final MessageCodec<ByteBuf, BroadcastMetadataExpirableValueMessage> CODEC = MessageCodec.ofMember(BroadcastMetadataExpirableValueMessage::write, BroadcastMetadataExpirableValueMessage::new);
    static final MessageIdentifier ID = new MessageIdentifier("metadata", "expirable_value");
    private final UUID uuid;
    private final String metadataId;
    private final byte[] data;
    private final Instant time;

    BroadcastMetadataExpirableValueMessage(UUID uuid, String metadataId, byte[] data, Instant time) {
        this.uuid = uuid;
        this.metadataId = metadataId;
        this.data = data;
        this.time = time;
    }

    private BroadcastMetadataExpirableValueMessage(ByteBuf buf) {
        super(buf);
        this.uuid = Util.readUUID(buf);
        this.metadataId = ByteBufHelper.readUtf8(buf, 32767);
        this.data = ByteBufHelper.readByteArray(buf, 64 * 1024 * 1024);
        this.time = Instant.ofEpochSecond(buf.readLong(), buf.readInt());
    }

    @Override
    protected void write(ByteBuf buf) {
        super.write(buf);
        Util.writeUUID(buf, this.uuid);
        ByteBufHelper.writeUtf8(buf, this.metadataId, 32767);
        ByteBufHelper.writeByteArray(buf, this.data);
        buf.writeLong(this.time.getEpochSecond());
        buf.writeInt(this.time.getNano());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    protected void handle() {
        MetaDataUser user = MetaDataProvider.get().getOptionalUser(this.uuid);
        // 不处理不在使用中的用户数据
        if (user == null) return;
        MetaData<?, ?> metaData = MetaDataProvider.get().getMetaData(this.metadataId);
        if (metaData == null || !metaData.expirable()) return;
        MetaDataValue metaDataValue = user.getValue(metaData);
        if (!(metaDataValue instanceof ExpirableMetaDataValue expirableMetaDataValue)) return;
        Object data = metaData.dataType().decode(this.data);
        expirableMetaDataValue.update(data, this.time, false);
    }

    @Override
    public @NotNull MessageIdentifier identifier() {
        return ID;
    }
}
