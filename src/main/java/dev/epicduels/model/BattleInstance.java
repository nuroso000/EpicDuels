package dev.epicduels.model;

import org.bukkit.World;

import java.util.UUID;

public interface BattleInstance {
    UUID getId();
    String getInstanceWorldName();
    World getInstanceWorld();
    boolean isActive();
    boolean isParticipant(UUID playerId);
    void recordPlayerBlock(int x, int y, int z);
    void removePlayerBlock(int x, int y, int z);
    boolean isPlayerPlacedBlock(int x, int y, int z);
}
