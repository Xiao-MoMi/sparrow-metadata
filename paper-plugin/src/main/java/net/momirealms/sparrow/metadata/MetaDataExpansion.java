package net.momirealms.sparrow.metadata;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MetaDataExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getAuthor() {
        return "XiaoMoMi";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "metadata";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        MetaDataManager metaDataManager = SparrowMetaData.instance().metaDataManager();
        MetaDataUser user = metaDataManager.getUser(player.getUniqueId());
        CrossServerDoubleMetaDataValue metaDataValue = user.getOrCreateValue(MetaDataConstants.DOUBLE);
        Double join = metaDataValue.lastKnownValue().join();
        return String.valueOf(join);
//        MetaDataRepo repo = MetaDataManager.instance().getOrCreateRepo(player.getUniqueId());
//        String[] split = params.split(":", 3);
//        boolean sync = false;
//        String content;
//        String defaultValue = null;
//        if (split.length == 1) {
//            content = split[0];
//        } else if (split.length == 2) {
//            sync = split[0].equals("0");
//            content = split[1];
//        } else {
//            sync = split[0].equals("0");
//            defaultValue = split[1];
//            content = split[2];
//        }
//        MetaData<? extends Tag> metaData = repo.getMetaData(content);
//        if (metaData == null) {
//            return defaultValue;
//        }
//        Tag tag;
//        if (sync) {
//            tag = metaData.get().join();
//        } else {
//            if (metaData.currentState() == MetaDataState.NOT_LOADED) {
//                metaData.get();
//                return defaultValue != null ? defaultValue : "数据加载中";
//            }
//            if (metaData.currentState() == MetaDataState.LOADING) {
//                return defaultValue != null ? defaultValue : "数据加载中";
//            }
//            if (metaData.currentState() == MetaDataState.EXPIRED) {
//                return defaultValue != null ? defaultValue : "";
//            }
//            Optional<? extends Tag> cachedValue = metaData.getCachedValue();
//            if (cachedValue.isEmpty()) {
//                return params + "数据加载出错";
//            }
//            tag = cachedValue.get();
//        }
//        return switch (tag) {
//            case DoubleTag doubleTag -> String.valueOf(doubleTag.getAsDouble());
//            case IntTag intTag -> String.valueOf(intTag.getAsInt());
//            case StringTag stringTag -> stringTag.getAsString();
//            case LongTag longTag -> String.valueOf(longTag.getAsLong());
//            case FloatTag floatTag -> String.valueOf(floatTag.getAsFloat());
//            case ByteTag byteTag -> String.valueOf(byteTag.getAsByte());
//            case null -> defaultValue != null ? defaultValue : "";
//            default -> "不支持的类型";
//        };
    }
}
