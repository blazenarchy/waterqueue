package dev.wnuke.waterqueue;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class PingCallback implements Callback<ServerPing> {
    private Queue queue;
    private ProxiedPlayer player;

    public PingCallback(Queue queue, ProxiedPlayer player) {
        this.queue = queue;
        this.player = player;
    }

    @Override
    public void done(ServerPing result, Throwable error) {
        if (result.getPlayers().getOnline() >= result.getPlayers().getMax()) {
            queue.join(player);
        } else {
            player.connect(queue.playServer);
        }
    }
}
