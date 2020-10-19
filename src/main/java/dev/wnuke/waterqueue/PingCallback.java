package dev.wnuke.waterqueue;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PingCallback implements Callback<ServerPing> {
    private final Queue queue;
    private final ProxiedPlayer player;

    public PingCallback(Queue queue, ProxiedPlayer player) {
        this.queue = queue;
        this.player = player;
    }

    @Override
    public void done(ServerPing result, Throwable error) {
        if (result != null) {
            if (result.getPlayers() != null) {
                if (result.getPlayers().getOnline() >= result.getPlayers().getMax()) {
                    Waterqueue.INSTANCE.logQueue(queue.name + " target server full sending player " + player.getName() + " to the queue.");
                    queue.join(player);
                } else {
                    Waterqueue.INSTANCE.logQueue(queue.name + " target server empty sending player " + player.getName() + " directly to the server.");
                    player.connect(queue.playServer);
                }
                return;
            }
        }
        Waterqueue.INSTANCE.logQueue(queue.name + " target server could not be pinged successfully, sending " + player.getName() + " to the queue.");
        queue.join(player);
    }
}
