package net.blazenarchy.waterqueue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
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
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                sendAllPlayersPos(false);
            }
        }, 0, 2000);
    }

    public void join(ProxiedPlayer player) {
        if (player == null) return;
        players.add(player);
        playerAddresses.add(player.getSocketAddress());
        if (player.getServer() != null) {
            if (player.getServer().getInfo() != queueServer) player.connect(queueServer);
        }
        Waterqueue.INSTANCE.logQueue(player.getName() + " has joined the \"" + name + "\" queue.");
        if (players.getFirst() == player) {
            playerTimeFirst.put(player.getUniqueId(), new Date().getTime());
        }
        sendQueuePos(player, true);
    }

    public int getPlayerCount() {
        return players.size();
    }


    public long getPlayerEta(ProxiedPlayer player, Long position) {
        return (position != null ? position : getQueuePos(player)) * averageTimeInFirst;
    }

    public int getQueuePos(ProxiedPlayer player) {
        return players.indexOf(player) + 1;
    }

    public void sendQueuePos(ProxiedPlayer player, boolean message) {
        long newAverage = Waterqueue.activeAverageMode ? (new Date().getTime() - timeLastLeft) : 0;
        for (long time : timesInFirst) {
            newAverage += time;
        }
        averageTimeInFirst = newAverage / (timesInFirst.size() + 1);
        long pos = getQueuePos(player);
        Waterqueue.sendPlayerQueueInfo(new QueuedPlayerInfo(player.getUniqueId(), getPlayerEta(player, pos), pos, name), this);
        if (message) player.sendMessage(new TextComponent(ChatColor.YELLOW + "Your position in queue is: " + pos));
    }

    private void removePlayer(ProxiedPlayer player) {
        if (players.contains(player)) {
            players.remove(player);
            playerAddresses.remove(player.getSocketAddress());
            playerTimeFirst.remove(player.getUniqueId());
            sendAllPlayersPos(true);
        }
    }

    private void sendAllPlayersPos(boolean message) {
        for (ProxiedPlayer otherPlayer : players) {
            sendQueuePos(otherPlayer, message);
        }
    }

    @EventHandler
    public void onDisconnect(ServerDisconnectEvent event) {
        removePlayer(event.getPlayer());
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        if (playerAddresses.contains(event.getSender().getSocketAddress())) event.setCancelled(true);
    }

    public void handleDisconnect() {
        timeLastLeft = new Date().getTime();
        if (players.size() > 0) {
            ProxiedPlayer ready = players.getFirst();
            if (ready != null && ready.getServer().getInfo() == queueServer) {
                ready.connect(playServer);
                playerAddresses.remove(ready.getSocketAddress());
                timesInFirst.add(timeLastLeft - playerTimeFirst.get(ready.getUniqueId()));
                removePlayer(ready);
                ProxiedPlayer newFirst = players.peekFirst();
                if (newFirst != null) playerTimeFirst.put(newFirst.getUniqueId(), timeLastLeft);
            }
        }
    }

    @Override
    public int compareTo(Queue o) {
        return this.priority.compareTo(o.priority);
    }
}
