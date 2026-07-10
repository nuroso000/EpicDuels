package dev.epicduels.model;

import java.util.UUID;

public class DuelRequest {

    private final UUID sender;
    private final UUID receiver;
    private final String arenaName;
    private final String kitName;
    private final int bestOf;
    private final long timestamp;

    public DuelRequest(UUID sender, UUID receiver, String arenaName, String kitName, int bestOf) {
        this.sender = sender;
        this.receiver = receiver;
        this.arenaName = arenaName;
        this.kitName = kitName;
        this.bestOf = bestOf;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getSender() {
        return sender;
    }

    public UUID getReceiver() {
        return receiver;
    }

    public String getArenaName() {
        return arenaName;
    }

    public String getKitName() {
        return kitName;
    }

    public int getBestOf() {
        return bestOf;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > 30_000;
    }
}
