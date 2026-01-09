package net.momirealms.sparrow.metadata;

import net.momirealms.sparrow.redis.messagebroker.MessageIdentifier;
import net.momirealms.sparrow.redis.messagebroker.codec.MessageCodec;
import net.momirealms.sparrow.redis.messagebroker.message.OneWayMessage;
import net.momirealms.sparrow.redis.messagebroker.util.SparrowByteBuf;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class BroadcastMetadataExpirableValueMessage extends OneWayMessage {
    static final MessageCodec<SparrowByteBuf, BroadcastMetadataExpirableValueMessage> CODEC = MessageCodec.ofMember(BroadcastMetadataExpirableValueMessage::write, BroadcastMetadataExpirableValueMessage::new);
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

    private BroadcastMetadataExpirableValueMessage(SparrowByteBuf buf) {
        super(buf);
        this.uuid = buf.readUUID();
        this.metadataId = buf.readUtf8();
        this.data = buf.readByteArray();
        this.time = Instant.ofEpochSecond(buf.readCompactLong(), buf.readCompactInt());
    }

    @Override
    protected void write(SparrowByteBuf buf) {
        super.write(buf);
        buf.writeUUID(this.uuid);
        buf.writeUtf8(this.metadataId);
        buf.writeByteArray(this.data);
        buf.writeCompactLong(time.getEpochSecond());
        buf.writeCompactInt(time.getNano());
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
