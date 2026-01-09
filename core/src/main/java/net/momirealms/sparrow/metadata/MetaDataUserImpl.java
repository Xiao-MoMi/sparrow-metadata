package net.momirealms.sparrow.metadata;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings({"unchecked","rawtypes"})
public class MetaDataUserImpl implements MetaDataUser {
    private final MetaDataManager manager;
    private final UserBindRepository userBindRepository;
    private final Map<MetaData, MetaDataValue> values;
    private final UUID uuid;
    private int timer;
    private boolean isOnline;
    private boolean loadedAll;

    protected MetaDataUserImpl(MetaDataManager manager, UUID uuid) {
        this.uuid = uuid;
        this.values = new IdentityHashMap<>();
        this.manager = manager;
        this.userBindRepository = new UserBindRepository(uuid, manager.repository());
    }

    public UUID uuid() {
        return this.uuid;
    }

    @Override
    public boolean isOnline() {
        return this.isOnline;
    }

    @Override
    public void setOnline(boolean online) {
        this.isOnline = online;
        if (!online) {
            this.loadedAll = false;
        }
    }

    @Override
    public boolean loaded() {
        return this.loadedAll;
    }

    @Override
    public MetaDataManager manager() {
        return this.manager;
    }

    @Override
    public UserBindRepository repository() {
        return this.userBindRepository;
    }

    @Override
    public void onTimer() {
        this.timer++;
        // 30 秒保存一次
        if (this.timer % 30 == 0) {
            this.saveDirty();
            this.manager.repository().lock(this.uuid);
        }
    }

    @Override
    public CompletableFuture<Void> saveDirty() {
        Map<String, List<FriendlyData<?>>> map = new HashMap<>();
        for (final Map.Entry<MetaData, MetaDataValue> entry : this.values.entrySet()) {
            if (entry.getValue() instanceof LazilyPersistedMetaDataValue<?> lazyValue) {
                List<FriendlyData<?>> data = map.computeIfAbsent(Optional.ofNullable(entry.getKey().collection()).orElse(this.manager.repository().defaultCollectionName()), k -> new ArrayList<>());
                lazyValue.saveIfDirty(data::add);
            }
        }
        if (!map.isEmpty()) {
            return this.manager.repository().update(this.uuid, map);
        }
        return CompletableFuture.completedFuture(null);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public CompletableFuture<Void> loadAll() {
        CompletableFuture<Map<String, Object>> all = this.repository().getAll(this.manager.collections());
        return all.thenAccept(it -> {
            for (MetaData metaData : this.manager.registeredMetaData()) {
                Object dataValue = it.get(metaData.id());
                Object value = null;
                if (dataValue != null) {
                    value = metaData.dataType().parse(dataValue);
                }
                MetaDataValue metaDataValue = this.values.get(metaData);
                if (metaDataValue != null) {
                    metaDataValue.update(value, false);
                } else {
                    MetaDataValue holder = metaData.createHolder(this);
                    holder.update(value, false);
                    this.values.put(metaData, holder);
                }
                if (metaDataValue instanceof ExpirableMetaDataValue<?> expirableValue) {
                    Date time = (Date) it.get(metaData.id() + ExpirableMetaDataValue.SUFFIX);
                    expirableValue.update(time.toInstant(), false);
                }
            }
            this.loadedAll = true;
        });
    }

    @Override
    public <T, H extends MetaDataValue<T>> H getOrCreateValue(MetaData<T, H> metaData) {
        return (H) this.values.computeIfAbsent(metaData, k -> metaData.createHolder(this));
    }

    @Override
    public <T, H extends MetaDataValue<T>> H getValue(MetaData<T, H> metaData) {
        return (H) this.values.get(metaData);
    }
}
