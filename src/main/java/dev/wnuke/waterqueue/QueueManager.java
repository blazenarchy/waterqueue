package dev.wnuke.waterqueue;

import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.HashMap;
import java.util.Map;

public class QueueManager implements Listener {
    public HashMap<String, Queue> queues;

    public QueueManager(HashMap<String, Queue> queues) {
        this.queues = queues;
        for (Queue queue : queues.values()) {
            Waterqueue.INSTANCE.getProxy().getPluginManager().registerListener(Waterqueue.INSTANCE, queue);
        }
    }

    @EventHandler
    public void onJoin(PostLoginEvent event) {
        for (Map.Entry<String, Queue> queueEntry : queues.entrySet()) {
            if (event.getPlayer().hasPermission("waterqueue.queues." + queueEntry.getKey())) {
                queueEntry.getValue().playServer.ping(new PingCallback(queueEntry.getValue(), event.getPlayer()));
            }
        }
    }
}
