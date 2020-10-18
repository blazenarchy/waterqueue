package dev.wnuke.waterqueue;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.LinkedList;

public class Queue implements Listener {
    public String name;
    public ServerInfo playServer;
    public ServerInfo queueServer;
    public int priority;
    private final LinkedList<ProxiedPlayer> players = new LinkedList<>();

    public Queue(String name, int priority, ServerInfo playServer, ServerInfo queueServer) {
        this.name = name;
        this.priority = priority;
        this.playServer = playServer;
        this.queueServer = queueServer;
    }

    public void join(ProxiedPlayer player) {
        players.add(player);
        player.connect(queueServer);
        sendQueuePos(player);
    }

    public void sendQueuePos(ProxiedPlayer player) {
        player.sendMessage(new TextComponent(ChatColor.YELLOW + "Your position in queue is: " + players.indexOf(player)));
    }

    @EventHandler
    public void onSwitch(ServerSwitchEvent event) {
        if (event.getFrom() == playServer) {
            players.getFirst().connect(playServer);
            players.removeFirst();
            for (ProxiedPlayer player : players) {
                sendQueuePos(player);
            }
        }
    }
}
