package net.blazenarchy.waterqueue;

import java.util.UUID;

public class QueuedPlayerInfo {
    UUID playerUuid;
    long eta;
    long position;
    String queue;

    public QueuedPlayerInfo(UUID playerUuid, long eta, long position, String queue) {
        this.playerUuid = playerUuid;
        this.eta = eta;
        this.position = position;
        this.queue = queue;
    }
}
