package net.blazenarchy.waterqueue;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

public class QueueManager implements Listener {
    private final HashMap<String, net.blazenarchy.waterqueue.Queue> queues;

    public QueueManager(HashMap<String, net.blazenarchy.waterqueue.Queue> queues) {
        this.queues = queues;
        for (net.blazenarchy.waterqueue.Queue queue : queues.values()) {
            Waterqueue.INSTANCE.getProxy().getPluginManager().registerListener(Waterqueue.INSTANCE, queue);
        }
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        StringBuilder permissions = new StringBuilder();
        for (String permission : event.getPlayer().getPermissions()) {
            permissions.append(permission).append(", ");
        }
        Waterqueue.INSTANCE.logQueue(event.getPlayer().getName() + " has the following permissions: " + permissions.toString());
        ArrayList<net.blazenarchy.waterqueue.Queue> playerQueues = new ArrayList<>();
        for (Map.Entry<String, net.blazenarchy.waterqueue.Queue> queueEntry : queues.entrySet()) {
            if (event.getPlayer().hasPermission("waterqueue.queue." + queueEntry.getKey())) {
                playerQueues.add(queueEntry.getValue());
            }
        }
        playerQueues.sort(net.blazenarchy.waterqueue.Queue::compareTo);
        if (!playerQueues.isEmpty()) {
            net.blazenarchy.waterqueue.Queue queue = playerQueues.get(playerQueues.size() - 1);
            queue.playServer.ping(new PingCallback(queue, event.getPlayer()));
            Waterqueue.INSTANCE.logQueue(event.getPlayer().getName() + " going into \"" + queue.name + "\" queue.");
        }
    }

    @EventHandler
    public void onDisconnect(ServerDisconnectEvent event) {
        ArrayList<net.blazenarchy.waterqueue.Queue> queuesForServer = new ArrayList<>();
        for (net.blazenarchy.waterqueue.Queue queue : queues.values()) {
            if (queue.playServer == event.getTarget()) {
                if (queue.getPlayerCount() > 0) {
                    queuesForServer.add(queue);
                }
            }
        }
        queuesForServer.sort(net.blazenarchy.waterqueue.Queue::compareTo);
        long longestWait = 0;
        long time = new Date().getTime();
        net.blazenarchy.waterqueue.Queue nextUp = null;
        for (Queue queue : queuesForServer) {
            long wait = (time - queue.timeLastLeft) * queue.priority;
            if (wait >= longestWait) {
                Waterqueue.INSTANCE.logQueue(queue.name + " has waited " + wait + " and the longest wait is " + longestWait + " (queue " + (nextUp != null ? nextUp.name : null) + ").");
                longestWait = wait;
                nextUp = queue;
            }
        }
        if (nextUp != null) nextUp.handleDisconnect();
    }
}
