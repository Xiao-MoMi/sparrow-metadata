package net.momirealms.sparrow.metadata;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import net.momirealms.sparrow.redis.messagebroker.Logger;
import net.momirealms.sparrow.redis.messagebroker.MessageBroker;
import net.momirealms.sparrow.redis.messagebroker.connection.PubSubRedisConnection;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.jsr310.InstantCodec;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class SparrowMetaData {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SparrowMetaData.class);
    private static SparrowMetaData instance;
    private final SparrowMetaDataPlugin plugin;
    private MetaDataManager metaDataManager;

    private MongoClient mongoClient;
    private RedisClient redisClient;

    protected SparrowMetaData(SparrowMetaDataPlugin plugin) {
        instance = this;
        this.plugin = plugin;
        this.createManager();
        this.registerDefaultMetaData();
    }

    private void registerDefaultMetaData() {
        this.metaDataManager.registerMetaData(MetaDataConstants.TEST_INT);
        this.metaDataManager.registerMetaData(MetaDataConstants.TEST_STR);
        this.metaDataManager.registerMetaData(MetaDataConstants.DOUBLE);
    }

    public SparrowMetaDataPlugin plugin() {
        return this.plugin;
    }

    protected void load() {
    }

    public void enable() {
        new MetaDataExpansion().register();
    }

    protected void disable() {
        if (this.mongoClient != null) {
            this.mongoClient.close();
        }
        if (this.redisClient != null) {
            this.redisClient.close();
        }
    }

    public MetaDataManager metaDataManager() {
        return this.metaDataManager;
    }

    public static SparrowMetaData instance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    private void createManager() {
        Yaml yaml = new Yaml(new StringKeyConfigConstructor(new LoaderOptions()));
        Path databasePath = this.plugin.getDataPath().resolve("database.yml");
        if (!Files.exists(databasePath)) {
            this.plugin.saveResource("database.yml", false);
        }
        try {
            Map<String, Object> data = yaml.load(Files.readString(databasePath, StandardCharsets.UTF_8));

            Map<String, Object> redis = (Map<String, Object>) data.get("redis");
            String redisUrl = (String) redis.get("url");
            String redisPrefix = (String) redis.get("prefix");

            Map<String, Object> mongodb = (Map<String, Object>) data.get("mongodb");
            String mongodbUri = (String) mongodb.get("uri");
            String mongodbDatabase = (String) mongodb.get("database");
            String mongodbCollection = (String) mongodb.get("collection");

            CodecRegistry pojoCodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder()
                            .automatic(true)
                            .register(InstantCodec.class)
                            .build())
            );

            this.mongoClient = MongoClients.create(MongoClientSettings.builder()
                    .uuidRepresentation(UuidRepresentation.STANDARD)
                    .applyConnectionString(new ConnectionString(mongodbUri))
                    .codecRegistry(pojoCodecRegistry)
                    .build());

            this.redisClient = RedisClient.create(redisUrl);
            this.redisClient.setOptions(ClientOptions.builder()
                    .autoReconnect(true)
                    .suspendReconnectOnProtocolFailure(false)
                    .requestQueueSize(10_000)
                    .disconnectedBehavior(ClientOptions.DisconnectedBehavior.ACCEPT_COMMANDS)
                    .build());

            this.metaDataManager = new MetaDataManagerImpl(
                    new RedisMongodbRepository(this.redisClient, this.mongoClient, mongodbDatabase, mongodbCollection, redisPrefix),
                    MessageBroker.builder()
                            .connection(new PubSubRedisConnection(this.redisClient))
                            .logger(new Logger() {
                                @Override
                                public void error(String s, Throwable t) {
                                    LOGGER.error(s, t);
                                }

                                @Override
                                public void warn(String s, Throwable t) {
                                    LOGGER.warn(s, t);
                                }

                                @Override
                                public void info(String s) {
                                    LOGGER.info(s);
                                }

                                @Override
                                public void debug(String s) {
                                    LOGGER.debug(s);
                                }
                            })
                            .channel("sparrow:metadata".getBytes(StandardCharsets.UTF_8))
                            .serverId("main")
                            .tags(Set.of("all"))
                            .build()
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load database.yml", e);
        }
    }
}
