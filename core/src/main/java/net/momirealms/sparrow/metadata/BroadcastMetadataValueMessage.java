package net.momirealms.sparrow.metadata;

import io.netty.buffer.ByteBuf;
import net.momirealms.sparrow.redis.messagebroker.MessageIdentifier;
import net.momirealms.sparrow.redis.messagebroker.codec.MessageCodec;
import net.momirealms.sparrow.redis.messagebroker.message.OneWayMessage;
import net.momirealms.sparrow.redis.messagebroker.util.ByteBufHelper;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class BroadcastMetadataValueMessage extends OneWayMessage<ByteBuf> {
    static final MessageCodec<ByteBuf, BroadcastMetadataValueMessage> CODEC = MessageCodec.ofMember(BroadcastMetadataValueMessage::write, BroadcastMetadataValueMessage::new);
    static final MessageIdentifier ID = new MessageIdentifier("metadata", "value");
    private final UUID uuid;
    private final String metadataId;
    private final byte[] data;

    BroadcastMetadataValueMessage(UUID uuid, String metadataId, byte[] data) {
        this.uuid = uuid;
        this.metadataId = metadataId;
        this.data = data;
    }

    private BroadcastMetadataValueMessage(ByteBuf buf) {
        super(buf);
        this.uuid = Util.readUUID(buf);
        this.metadataId = ByteBufHelper.readUtf8(buf, 32767);
        this.data = ByteBufHelper.readByteArray(buf, 64 * 1024 * 1024);
    }

    @Override
    protected void write(ByteBuf buf) {
        super.write(buf);
        Util.writeUUID(buf, uuid);
        ByteBufHelper.writeUtf8(buf, this.metadataId, 32767);
        ByteBufHelper.writeByteArray(buf, this.data);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void handle() {
        MetaDataUser user = MetaDataProvider.get().getOptionalUser(this.uuid);
        // 不处理不在使用中的用户数据
        if (user == null) return;
        MetaData<?, ?> metaData = MetaDataProvider.get().getMetaData(this.metadataId);
        if (metaData == null) return;
        MetaDataValue metaDataValue = user.getValue(metaData);
        if (metaDataValue == null) return;
        Object data = metaData.dataType().decode(this.data);
        metaDataValue.update(data, false);
    }

    @Override
    public @NotNull MessageIdentifier identifier() {
        return ID;
    }
}
