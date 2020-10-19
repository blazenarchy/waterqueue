package dev.wnuke.waterqueue;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.google.gson.reflect.TypeToken;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.HashSet;
import java.util.UUID;

public class Placeholders extends PlaceholderExpansion implements PluginMessageListener {
    private final HashSet<QueuedPlayerInfo> players = new HashSet<>();

    public void removePlayer(UUID playerUuid) {
        players.removeIf(info -> info.playerUuid.equals(playerUuid));
    }

    @Override
    public String getIdentifier() {
        return "waterqueue";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
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
    public String onPlaceholderRequest(Player player, String params) {
        long value = -1;
        QueuedPlayerInfo playerInfo = null;
        for (QueuedPlayerInfo info : players) {
            if (info.playerUuid.equals(player.getUniqueId())) {
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
        switch (params) {
            case "pos":
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
        if (!Global.INFO_CHANNEL.equals(channel)) return;

        //noinspection UnstableApiUsage
        final ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String input = in.readUTF();
        QueuedPlayerInfo newPlayerInfo = Global.gson.fromJson(input, new TypeToken<QueuedPlayerInfo>() {
        }.getType());
        removePlayer(newPlayerInfo.playerUuid);
        players.add(newPlayerInfo);
    }
}
