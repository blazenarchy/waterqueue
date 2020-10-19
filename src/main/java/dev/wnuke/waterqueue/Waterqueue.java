package dev.wnuke.waterqueue;

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
            e.printStackTrace();
            onDisable();
            return;
        }
        if (config.getBoolean("queue_logs")) logQueue = true;
        Configuration queueConfigs = config.getSection("queues");
        if (queueConfigs == null || queueConfigs.getKeys().isEmpty()) {
            getLogger().severe("Waterqueue has no queues specified in it's configuration.");
        } else {
            defaultPlayServer = getProxy().getServerInfo(config.getString("default_play_server"));
            if (defaultPlayServer == null) {
                getLogger().severe("The specified default play server does not exist.");
            }
            defaultQueueServer = getProxy().getServerInfo(config.getString("default_queue_server"));
            if (defaultQueueServer == null) {
                getLogger().severe("The specified default queue server does not exist.");
            }
            HashMap<String, Queue> queues = new HashMap<>();
            loopConfigs:
            for (String queueName : queueConfigs.getKeys()) {
                Configuration queueConfig = queueConfigs.getSection(queueName);
                String playServerName = queueConfigs.getString("play_server");
                ServerInfo playServer = getProxy().getServerInfo(playServerName);
                if (playServerName.isEmpty() || playServer == null) {
                    if (playServer == null)
                        getLogger().warning("Specified play server for " + queueName + " does not exist, using default.");
                    if (defaultPlayServer == null) {
                        getLogger().warning("Default play server not specified, skipping " + queueName + ".");
                        continue;
                    }
                    playServer = defaultPlayServer;
                }
                String queueServerName = queueConfigs.getString("play_server");
                ServerInfo queueServer = getProxy().getServerInfo(queueServerName);
                if (queueServerName.isEmpty() || queueServer == null) {
                    if (queueServer == null)
                        getLogger().warning("Specified queue server for " + queueName + " does not exist, using default.");
                    if (defaultQueueServer == null) {
                        getLogger().warning("Default queue server not specified, skipping " + queueName + ".");
                        continue;
                    }
                    queueServer = defaultQueueServer;
                }
                int priority = queueConfig.getInt("priority");
                for (Queue queue : queues.values()) {
                    if (queue.priority == priority) {
                        getLogger().warning(queueName + " has the same priority as " + queue.name + ", skipping it.");
                        continue loopConfigs;
                    }
                }
                Queue queue = new Queue(queueName, priority, playServer, queueServer);
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
