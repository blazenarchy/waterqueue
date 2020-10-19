package dev.wnuke.waterqueue;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashSet;

public class Placeholders extends PlaceholderExpansion implements PluginMessageListener {
    public static final String INFO_CHANNEL = "QueueInfoChannel";
    private static final Gson gson = new GsonBuilder().create();
    private final HashSet<QueuedPlayerInfo> players = new HashSet<>();

    public static void sendPlayerQueueInfo(QueuedPlayerInfo playerInfo, Queue queue) {
        //noinspection UnstableApiUsage
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(gson.toJson(playerInfo));
        queue.queueServer.sendData(INFO_CHANNEL, out.toByteArray());
    }

    @Override
    public String getIdentifier() {
        return "waterqueue";
    }

    @Override
    public String getAuthor() {
        return "wnuke";
    }

    @Override
    public String getVersion() {
        return WaterqueueCompanion.INSTANCE.getDescription().getVersion();
    }

    @Override
    public String onRequest(final OfflinePlayer player, String identifier) {
        long value = -1;
        if (player == null) return "";
        QueuedPlayerInfo playerInfo = null;
        for (QueuedPlayerInfo info : players) {
            if (info.playerUuid == player.getUniqueId()) {
                playerInfo = info;
                break;
            }
        }
        if (playerInfo == null) return "";
        long millis = playerInfo.eta;
        String eta;
        long minutes = millis / 1000 / 60;
        if (minutes < 60) eta = String.valueOf(minutes);
        else {
            double hours = minutes / 60d;
            eta = String.valueOf(hours);
        }
        switch (identifier) {
            case "position":
                value = playerInfo.position;
                break;
            case "eta":
                return eta;
            case "queue":
                return playerInfo.queue;
        }
        return String.valueOf(value);
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message) {
        if (!INFO_CHANNEL.equals(channel)) return;

        //noinspection UnstableApiUsage
        final ByteArrayDataInput in = ByteStreams.newDataInput(message);
        QueuedPlayerInfo newPlayerInfo = gson.fromJson(in.readUTF(), new TypeToken<QueuedPlayerInfo>() {
        }.getType());
        players.removeIf(info -> info.playerUuid == newPlayerInfo.playerUuid);
        players.add(newPlayerInfo);
    }
}
