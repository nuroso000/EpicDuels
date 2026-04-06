package dev.epicduels.model;

import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DuelInstance {

    private final UUID id;
    private final UUID player1;
    private final UUID player2;
    private final String arenaName;
    private final String kitName;
    private final String instanceWorldName;
    private World instanceWorld;
    private boolean active;
    private boolean countdownComplete;
    // Tracks blocks placed by players during the duel - these may be broken freely.
    // All other blocks are original map blocks and cannot be broken.
    private final Set<Long> playerPlacedBlocks = new HashSet<>();

    public DuelInstance(UUID player1, UUID player2, String arenaName, String kitName) {
        this.id = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.arenaName = arenaName;
        this.kitName = kitName;
        this.instanceWorldName = "arena_instance_" + arenaName + "_" + id.toString().substring(0, 8);
        this.active = false;
        this.countdownComplete = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayer1() {
        return player1;
    }

    public UUID getPlayer2() {
        return player2;
    }

    public String getArenaName() {
        return arenaName;
    }

    public String getKitName() {
        return kitName;
    }

    public String getInstanceWorldName() {
        return instanceWorldName;
    }

    public World getInstanceWorld() {
        return instanceWorld;
    }

    public void setInstanceWorld(World instanceWorld) {
        this.instanceWorld = instanceWorld;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isCountdownComplete() {
        return countdownComplete;
    }

    public void setCountdownComplete(boolean countdownComplete) {
        this.countdownComplete = countdownComplete;
    }

    public boolean isParticipant(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        return player1.equals(uuid) ? player2 : player1;
    }

    public void recordPlayerBlock(int x, int y, int z) {
        playerPlacedBlocks.add(encodeBlockPos(x, y, z));
    }

    public void removePlayerBlock(int x, int y, int z) {
        playerPlacedBlocks.remove(encodeBlockPos(x, y, z));
    }

    public boolean isPlayerPlacedBlock(int x, int y, int z) {
        return playerPlacedBlocks.contains(encodeBlockPos(x, y, z));
    }

    private static long encodeBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) << 38 | ((long) (y & 0xFFF)) << 26 | ((long) (z & 0x3FFFFFF));
    }
}
