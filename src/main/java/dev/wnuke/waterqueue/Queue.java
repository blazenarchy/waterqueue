package dev.wnuke.waterqueue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.SocketAddress;
import java.util.*;

public class Queue implements Listener, Comparable<Queue> {
    private final HashSet<Long> timesInFirst = new HashSet<>();
    private final HashMap<UUID, Long> playerTimeFirst = new HashMap<>();
    private final LinkedList<ProxiedPlayer> players = new LinkedList<>();
    private final HashSet<SocketAddress> playerAddresses = new HashSet<>();
    public String name;
    public ServerInfo playServer;
    public ServerInfo queueServer;
    public long timeLastLeft = 0;
    public long averageTimeInFirst = 0;
    public Integer priority;

    public Queue(String name, int priority, ServerInfo playServer, ServerInfo queueServer) {
        this.name = name;
        this.priority = priority;
        this.playServer = playServer;
        this.queueServer = queueServer;
    }

    public void join(ProxiedPlayer player) {
        players.add(player);
        playerAddresses.add(player.getSocketAddress());
        if (player.getServer().getInfo() != queueServer) player.connect(queueServer);
        Waterqueue.INSTANCE.logQueue(player.getName() + " has joined the \"" + name + "\" queue.");
        if (players.getFirst() == player) {
            playerTimeFirst.put(player.getUniqueId(), new Date().getTime());
        }
        long pos = getQueuePos(player);
        sendQueuePos(player, pos);
        Waterqueue.sendPlayerQueueInfo(new QueuedPlayerInfo(player.getUniqueId(), getPlayerEta(player, pos), pos, name), this);
    }

    public int getPlayerCount() {
        return players.size();
    }


    public long getPlayerEta(ProxiedPlayer player, Long position) {
        return position != null ? position : getQueuePos(player) * averageTimeInFirst;
    }

    public int getQueuePos(ProxiedPlayer player) {
        return players.indexOf(player) + 1;
    }

    public void sendQueuePos(ProxiedPlayer player, Long position) {
        player.sendMessage(new TextComponent(ChatColor.YELLOW + "Your position in queue is: " + (position != null ? position : getQueuePos(player))));
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (playerAddresses.contains(event.getSender().getSocketAddress())) event.setCancelled(true);
    }

    public void handleDisconnect() {
        timeLastLeft = new Date().getTime();
        if (players.size() > 0) {
            ProxiedPlayer first = players.getFirst();
            if (first != null) {
                first.connect(playServer);
                playerAddresses.remove(first.getSocketAddress());
                players.removeFirst();
                timesInFirst.add(timeLastLeft - playerTimeFirst.get(first.getUniqueId()));
                ProxiedPlayer newFirst = players.peekFirst();
                if (newFirst != null) playerTimeFirst.put(newFirst.getUniqueId(), new Date().getTime());
                int newAverage = 0;
                for (long time : timesInFirst) {
                    newAverage += time;
                }
                averageTimeInFirst = newAverage / timesInFirst.size();
                for (ProxiedPlayer player : players) {
                    long pos = getQueuePos(player);
                    sendQueuePos(player, pos);
                    Waterqueue.sendPlayerQueueInfo(new QueuedPlayerInfo(player.getUniqueId(), getPlayerEta(player, pos), pos, name), this);
                }
            }
        }
    }

    @Override
    public int compareTo(Queue o) {
        return this.priority.compareTo(o.priority);
    }
}
