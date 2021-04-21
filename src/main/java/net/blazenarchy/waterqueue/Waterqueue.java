package net.blazenarchy.waterqueue;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;


public final class Waterqueue extends Plugin {
    public static boolean logQueue = false;
    public static boolean activeAverageMode = false;
    public static Waterqueue INSTANCE;
    public static Configuration config;
    public static QueueManager manager;
    public ServerInfo defaultQueueServer;
    public ServerInfo defaultPlayServer;

    public static void sendPlayerQueueInfo(QueuedPlayerInfo playerInfo, Queue queue) {
        //noinspection UnstableApiUsage
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(Global.gson.toJson(playerInfo));
        queue.queueServer.sendData(Global.INFO_CHANNEL, out.toByteArray());
    }

    @Override
    public void onEnable() {
        getLogger().info("Loading Waterqueue by wnuke...");
        File configFile = new File(getDataFolder(), "config.yml");
        INSTANCE = this;
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        if (!configFile.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, configFile.toPath());
            } catch (IOException e) {
                getLogger().severe("Waterqueue failed to load, could not save default configuration.");
                e.printStackTrace();
                onDisable();
                return;
            }
        }
        try {
            config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
        } catch (IOException e) {
            getLogger().severe("Waterqueue failed to load, could not read configuration.");
            return;
        }
        if (config.getBoolean("queue_logs")) logQueue = true;
        if (config.getBoolean("active_queue")) activeAverageMode = true;
        Configuration queueConfigs = config.getSection("queues");
        if (queueConfigs == null || queueConfigs.getKeys().isEmpty()) {
            getLogger().severe("Waterqueue failed to load, no queues were specified in it's configuration.");
            return;
        } else {
            defaultPlayServer = getProxy().getServerInfo(config.getString("default_play_server"));
            if (defaultPlayServer == null) {
                getLogger().severe("Waterqueue failed to load, the specified default play server does not exist.");
                return;
            }
            defaultQueueServer = getProxy().getServerInfo(config.getString("default_queue_server"));
            if (defaultQueueServer == null) {
                getLogger().severe("Waterqueue failed to load, the specified default queue server does not exist.");
                return;
            }
            HashMap<String, Queue> queues = new HashMap<>();
            for (String queueName : queueConfigs.getKeys()) {
                int priority = queueConfigs.getInt(queueName);
                Queue queue = new Queue(queueName, priority, defaultPlayServer, defaultQueueServer);
                queues.put(queueName, queue);
            }
            manager = new QueueManager(queues);
            getProxy().getPluginManager().registerListener(this, manager);
        }
        getLogger().info("Waterqueue by wnuke is loaded.");
    }

    public void logQueue(String message) {
        if (logQueue) getLogger().info(message);
    }
}
