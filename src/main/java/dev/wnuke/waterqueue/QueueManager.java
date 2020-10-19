package dev.wnuke.waterqueue;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.ServerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

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
        StringBuilder permissions = new StringBuilder();
        for (String permission : event.getPlayer().getPermissions()) {
            permissions.append(permission).append(", ");
        }
        Waterqueue.INSTANCE.logQueue(event.getPlayer().getName() + " has the following permissions: " + permissions.toString());
        ArrayList<Queue> playerQueues = new ArrayList<>();
        for (Map.Entry<String, Queue> queueEntry : queues.entrySet()) {
            if (event.getPlayer().hasPermission("waterqueue.queue." + queueEntry.getKey())) {
                playerQueues.add(queueEntry.getValue());
            }
        }
        playerQueues.sort(Queue::compareTo);
        if (!playerQueues.isEmpty()) {
            Queue queue = playerQueues.get(playerQueues.size() - 1);
            queue.playServer.ping(new PingCallback(queue, event.getPlayer()));
            Waterqueue.INSTANCE.logQueue(event.getPlayer().getName() + " going into \"" + queue.name + "\" queue.");
        }
    }

    public long getPlayerQueueAverage(UUID playerID) {
        ProxiedPlayer player = Waterqueue.INSTANCE.getProxy().getPlayer(playerID);
        if (player == null) return 0;
        for (Queue queue : queues.values()) {
            if (queue.playerInQueue(player)) {
                return queue.averageTimeInFirst;
            }
        }
        return 0;
    }

    public String getPlayerQueue(UUID playerID) {
        ProxiedPlayer player = Waterqueue.INSTANCE.getProxy().getPlayer(playerID);
        if (player == null) return "";
        for (Queue queue : queues.values()) {
            if (queue.playerInQueue(player)) {
                return queue.name;
            }
        }
        return "";
    }


    public int getPlayerPosition(UUID playerID) {
        ProxiedPlayer player = Waterqueue.INSTANCE.getProxy().getPlayer(playerID);
        if (player == null) return -1;
        for (Queue queue : queues.values()) {
            if (queue.playerInQueue(player)) {
                return queue.getQueuePos(player);
            }
        }
        return -1;
    }

    @EventHandler
    public void onDisconnect(ServerDisconnectEvent event) {
        ArrayList<Queue> queuesForServer = new ArrayList<>();
        for (Queue queue : queues.values()) {
            if (queue.playServer == event.getTarget()) {
                if (queue.getPlayerCount() > 0) {
                    queuesForServer.add(queue);
                }
            }
        }
        queuesForServer.sort(Queue::compareTo);
        long longestWait = 0;
        long time = new Date().getTime();
        Queue nextUp = null;
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
