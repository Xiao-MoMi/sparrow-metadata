package net.momirealms.sparrow.metadata;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class SparrowMetaDataPlugin extends JavaPlugin implements Listener {
    private final SparrowMetaDataBootstrap bootstrap;
    private SparrowMetaData core;

    protected SparrowMetaDataPlugin(SparrowMetaDataBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public void onLoad() {
        this.core = new SparrowMetaData(this);
        this.core.load();
    }

    @Override
    public void onEnable() {
        this.core.enable();
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.core.disable();
        HandlerList.unregisterAll((Listener) this);
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onConfiguration(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();
        UUID uuid = connection.getProfile().getId();
        System.out.println("[SparrowMetaDataPlugin] " + uuid + " 配置阶段开始");
        MetaDataUser onlineUser = this.core.metaDataManager().createOnlineUser(uuid, connection::isConnected, true);
        onlineUser.loadAll().join();
        System.out.println("[SparrowMetaDataPlugin] " + uuid + " 配置阶段加载完成");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerConnectionCloseEvent event) {
        UUID uuid = event.getPlayerUniqueId();
        this.core.metaDataManager().removeOnlineUser(uuid, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        MetaDataUser user = this.core.metaDataManager().getUser(player.getUniqueId());
        Bukkit.getScheduler().runTaskTimer(this, (task) -> {
            if (!player.isOnline()) {
                task.cancel();
                return;
            }

            ExpirableMetaDataValue<Integer> value = user.getOrCreateValue(MetaDataConstants.TEST_INT);
            value.update(ThreadLocalRandom.current().nextInt(1000), Instant.now().plus(3, ChronoUnit.SECONDS),true);
            user.onTimer();
        }, 1, 1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(PlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        MetaDataUser user = this.core.metaDataManager().getUser(player.getUniqueId());
        if (message.contains("1")) {

        } else if (message.contains("2")) {
            CommonMetaDataValue<String> orCreateValue = user.getOrCreateValue(MetaDataConstants.TEST_STR);
            orCreateValue.update(String.valueOf(ThreadLocalRandom.current().nextInt(1000)));
        }
    }

    public SparrowMetaDataBootstrap bootstrap() {
        return this.bootstrap;
    }
}
