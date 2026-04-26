package dev.epicduels.model;

import org.bukkit.World;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class TeamDuelInstance implements BattleInstance {

    public enum Team { A, B }

    private final UUID id;
    private final Set<UUID> teamA;
    private final Set<UUID> teamB;
    private final Set<UUID> aliveA;
    private final Set<UUID> aliveB;
    private final String arenaName;
    private final String kitName;
    private final String instanceWorldName;
    private World instanceWorld;
    private boolean active;
    private boolean countdownComplete;
    private final Set<Long> playerPlacedBlocks = new HashSet<>();

    public TeamDuelInstance(Set<UUID> teamA, Set<UUID> teamB, String arenaName, String kitName) {
        this.id = UUID.randomUUID();
        this.teamA = new LinkedHashSet<>(teamA);
        this.teamB = new LinkedHashSet<>(teamB);
        this.aliveA = new LinkedHashSet<>(teamA);
        this.aliveB = new LinkedHashSet<>(teamB);
        this.arenaName = arenaName;
        this.kitName = kitName;
        this.instanceWorldName = "arena_instance_" + arenaName + "_" + id.toString().substring(0, 8);
        this.active = false;
        this.countdownComplete = false;
    }

    @Override public UUID getId() { return id; }
    @Override public String getInstanceWorldName() { return instanceWorldName; }
    @Override public World getInstanceWorld() { return instanceWorld; }
    public void setInstanceWorld(World w) { this.instanceWorld = w; }
    @Override public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isCountdownComplete() { return countdownComplete; }
    public void setCountdownComplete(boolean v) { this.countdownComplete = v; }
    public String getArenaName() { return arenaName; }
    public String getKitName() { return kitName; }

    public Set<UUID> getTeamA() { return teamA; }
    public Set<UUID> getTeamB() { return teamB; }
    public Set<UUID> getAliveA() { return aliveA; }
    public Set<UUID> getAliveB() { return aliveB; }

    public Set<UUID> getAllParticipants() {
        Set<UUID> all = new LinkedHashSet<>(teamA);
        all.addAll(teamB);
        return all;
    }

    @Override
    public boolean isParticipant(UUID playerId) {
        return teamA.contains(playerId) || teamB.contains(playerId);
    }

    public Team getTeamOf(UUID playerId) {
        if (teamA.contains(playerId)) return Team.A;
        if (teamB.contains(playerId)) return Team.B;
        return null;
    }

    public boolean sameTeam(UUID a, UUID b) {
        Team ta = getTeamOf(a);
        Team tb = getTeamOf(b);
        return ta != null && ta == tb;
    }

    public void markDead(UUID playerId) {
        aliveA.remove(playerId);
        aliveB.remove(playerId);
    }

    public boolean isAlive(UUID playerId) {
        return aliveA.contains(playerId) || aliveB.contains(playerId);
    }

    public boolean isTeamWiped(Team team) {
        return team == Team.A ? aliveA.isEmpty() : aliveB.isEmpty();
    }

    public Team getWinningTeam() {
        if (aliveA.isEmpty() && !aliveB.isEmpty()) return Team.B;
        if (aliveB.isEmpty() && !aliveA.isEmpty()) return Team.A;
        return null;
    }

    public Set<UUID> getTeam(Team team) {
        return team == Team.A ? teamA : teamB;
    }

    @Override
    public void recordPlayerBlock(int x, int y, int z) {
        playerPlacedBlocks.add(encodeBlockPos(x, y, z));
    }

    @Override
    public void removePlayerBlock(int x, int y, int z) {
        playerPlacedBlocks.remove(encodeBlockPos(x, y, z));
    }

    @Override
    public boolean isPlayerPlacedBlock(int x, int y, int z) {
        return playerPlacedBlocks.contains(encodeBlockPos(x, y, z));
    }

    private static long encodeBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) << 38 | ((long) (y & 0xFFF)) << 26 | ((long) (z & 0x3FFFFFF));
    }
}
