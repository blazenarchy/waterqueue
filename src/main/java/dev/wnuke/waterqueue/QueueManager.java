package dev.wnuke.waterqueue;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class QueueManager implements Listener {
    private final HashMap<String, Queue> queues;

    public QueueManager(HashMap<String, Queue> queues) {
        this.queues = queues;
        for (Queue queue : queues.values()) {
            Waterqueue.INSTANCE.getProxy().getPluginManager().registerListener(Waterqueue.INSTANCE, queue);
        }
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        for (Map.Entry<String, Queue> queueEntry : queues.entrySet()) {
            if (event.getPlayer().hasPermission("waterqueue.queue." + queueEntry.getKey())) {
                queueEntry.getValue().playServer.ping(new PingCallback(queueEntry.getValue(), event.getPlayer()));
            }
        }
    }

    @EventHandler
    public void onDisconnect(ServerDisconnectEvent event) {
        HashSet<Queue> queuesForServer = new HashSet<>();
        Queue lowestPriority = null;
        for (Queue queue : queues.values()) {
            if (queue.playServer == event.getTarget()) {
                if (queue.getPlayerCount() > 0) {
                    queuesForServer.add(queue);
                    if (lowestPriority == null) lowestPriority = queue;
                    else if (queue.priority < lowestPriority.priority) lowestPriority = queue;
                }
            }
        }
        Queue worstRatio = null;
        int worstDifference = 0;
        for (Queue queue : queuesForServer) {
            int ratio = lowestPriority.priority / queue.priority;
            int playerRatio = queue.getPlayerCount() / lowestPriority.getPlayerCount();
            int difference = playerRatio - ratio;
            if (difference > worstDifference) {
                worstDifference = difference;
                worstRatio = queue;
            }
        }
        if (worstRatio != null) {
            worstRatio.handleDisconnect(event.getTarget());
        }
    }
}
