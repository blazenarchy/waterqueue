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
        long seconds = playerInfo.eta / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        String s = String.valueOf(seconds % 60);
        String m = String.valueOf(minutes % 60);
        String h = String.valueOf(hours % 24);
        if (s.length() == 1) {
            if (seconds < 10) {
                s = "0" + s;
            } else {
                s = s + "0";
            }
        }
        if (m.length() == 1) {
            if (minutes < 10) {
                m = "0" + m;
            } else {
                m = m + "0";
            }
        }
        if (h.length() == 1) {
            if (hours < 10) {
                h = "0" + h;
            } else {
                h = h + "0";
            }
        }
        String time;
        if (hours > 0) {
            time = h + "h " + m + "m";
        } else {
            time = m + "m " + s + "s";
        }
        switch (params) {
            case "pos":
                value = playerInfo.position;
                break;
            case "eta":
                return time;
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
