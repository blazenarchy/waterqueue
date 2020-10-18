package dev.wnuke.waterqueue;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.net.SocketAddress;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;

public class Queue implements Listener, Comparable<Queue> {
    public String name;
    public ServerInfo playServer;
    public ServerInfo queueServer;
    public long timeLastLeft = 0;
    public Integer priority;
    private final LinkedList<ProxiedPlayer> players = new LinkedList<>();
    private final HashSet<SocketAddress> playerAddresses = new HashSet<>();

    public Queue(String name, int priority, ServerInfo playServer, ServerInfo queueServer) {
        this.name = name;
        this.priority = priority;
        this.playServer = playServer;
        this.queueServer = queueServer;
    }

    public void join(ProxiedPlayer player) {
        Waterqueue.INSTANCE.getLogger().info(player.getName() + " has joined the \"" + name + "\" queue.");
        players.add(player);
        playerAddresses.add(player.getSocketAddress());
        player.connect(queueServer);
        sendQueuePos(player);
    }

    public int getPlayerCount() {
        return players.size();
    }

    public void sendQueuePos(ProxiedPlayer player) {
        player.sendMessage(new TextComponent(ChatColor.YELLOW + "Your position in queue is: " + players.indexOf(player)));
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
                for (ProxiedPlayer player : players) {
                    sendQueuePos(player);
                }
            }
        }
    }

    @Override
    public int compareTo(Queue o) {
        return this.priority.compareTo(o.priority);
    }
}
