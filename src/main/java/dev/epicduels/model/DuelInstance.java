package dev.epicduels.model;

import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DuelInstance implements BattleInstance {

    private final UUID id;
    private final UUID player1;
    private final UUID player2;
    private final String arenaName;
    private final String kitName;
    private final String instanceWorldName;
    private World instanceWorld;
    private boolean active;
    private boolean countdownComplete;
    // Time-limit deadline (epoch millis); 0 = no limit. Set when the countdown finishes.
    private long deadlineMillis;
    // True once the match entered its one-time sudden-death extension (tournament matches).
    private boolean suddenDeath;
    // Best-of-N round state: first player to bestOf/2+1 round wins takes the match.
    private final int bestOf;
    private int wins1;
    private int wins2;
    private int currentRound = 1;
    // Tracks blocks placed by players during the duel - these may be broken freely.
    // All other blocks are original map blocks and cannot be broken.
    private final Set<Long> playerPlacedBlocks = new HashSet<>();

    public DuelInstance(UUID player1, UUID player2, String arenaName, String kitName) {
        this(player1, player2, arenaName, kitName, 1);
    }

    public DuelInstance(UUID player1, UUID player2, String arenaName, String kitName, int bestOf) {
        this.id = UUID.randomUUID();
        this.player1 = player1;
        this.player2 = player2;
        this.arenaName = arenaName;
        this.kitName = kitName;
        this.bestOf = Math.max(1, bestOf);
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

    public long getDeadlineMillis() {
        return deadlineMillis;
    }

    public void setDeadlineMillis(long deadlineMillis) {
        this.deadlineMillis = deadlineMillis;
    }

    public boolean isSuddenDeath() {
        return suddenDeath;
    }

    public void setSuddenDeath(boolean suddenDeath) {
        this.suddenDeath = suddenDeath;
    }

    public int getBestOf() {
        return bestOf;
    }

    public int getRoundsToWin() {
        return bestOf / 2 + 1;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void addRoundWin(UUID playerId) {
        if (player1.equals(playerId)) {
            wins1++;
        } else {
            wins2++;
        }
    }

    public int getWins(UUID playerId) {
        return player1.equals(playerId) ? wins1 : wins2;
    }

    /**
     * Advances to the next round: resets countdown, clock and sudden-death
     * state. The instance world is reused.
     */
    public void startNextRound() {
        currentRound++;
        countdownComplete = false;
        deadlineMillis = 0;
        suddenDeath = false;
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

    /**
     * Decoded positions of all player-placed blocks, as {x, y, z} triples.
     * Used to reset the arena between best-of-N rounds.
     */
    public List<int[]> getPlayerPlacedBlockPositions() {
        List<int[]> positions = new ArrayList<>(playerPlacedBlocks.size());
        for (long encoded : playerPlacedBlocks) {
            positions.add(new int[]{
                    (int) (encoded >> 38),
                    (int) (encoded << 26 >> 52),
                    (int) (encoded << 38 >> 38)
            });
        }
        return positions;
    }

    public void clearPlayerPlacedBlocks() {
        playerPlacedBlocks.clear();
    }

    private static long encodeBlockPos(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF)) << 38 | ((long) (y & 0xFFF)) << 26 | ((long) (z & 0x3FFFFFF));
    }
}
