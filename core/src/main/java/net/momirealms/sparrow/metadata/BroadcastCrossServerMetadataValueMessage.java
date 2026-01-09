package net.momirealms.sparrow.metadata;

import net.momirealms.sparrow.redis.messagebroker.MessageIdentifier;
import net.momirealms.sparrow.redis.messagebroker.codec.MessageCodec;
import net.momirealms.sparrow.redis.messagebroker.message.OneWayMessage;
import net.momirealms.sparrow.redis.messagebroker.util.SparrowByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class BroadcastCrossServerMetadataValueMessage extends OneWayMessage {
    static final MessageCodec<SparrowByteBuf, BroadcastCrossServerMetadataValueMessage> CODEC = MessageCodec.ofMember(BroadcastCrossServerMetadataValueMessage::write, BroadcastCrossServerMetadataValueMessage::new);
    static final MessageIdentifier ID = new MessageIdentifier("metadata", "cross_server_value");
    private final UUID uuid;
    private final String metadataId;
    private final byte[] data;
    private final long timestamp;

    BroadcastCrossServerMetadataValueMessage(UUID uuid, String metadataId, byte[] data, long timestamp) {
        this.uuid = uuid;
        this.metadataId = metadataId;
        this.data = data;
        this.timestamp = timestamp;
    }

    private BroadcastCrossServerMetadataValueMessage(SparrowByteBuf buf) {
        super(buf);
        this.uuid = buf.readUUID();
        this.metadataId = buf.readUtf8();
        this.data = buf.readByteArray();
        this.timestamp = buf.readCompactLong();
    }

    @Override
    protected void write(SparrowByteBuf buf) {
        super.write(buf);
        buf.writeUUID(this.uuid);
        buf.writeUtf8(this.metadataId);
        buf.writeByteArray(this.data);
        buf.writeCompactLong(this.timestamp);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void handle() {
        MetaDataUser user = MetaDataProvider.get().getOptionalUser(this.uuid);
        // 不处理不在使用中的用户数据
        if (user == null) return;
        MetaData<?, ?> metaData = MetaDataProvider.get().getMetaData(this.metadataId);
        if (metaData == null || !metaData.crossServerSync()) return;
        MetaDataValue metaDataValue = user.getValue(metaData);
        if (!(metaDataValue instanceof CrossServerMetaDataValue crossServerMetaDataValue)) return;
        Object data = metaData.dataType().decode(this.data);
        crossServerMetaDataValue.update(data, this.timestamp, false);
    }

    @Override
    public @NotNull MessageIdentifier identifier() {
        return ID;
    }
}
