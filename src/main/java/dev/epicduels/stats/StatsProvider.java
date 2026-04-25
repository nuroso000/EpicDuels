package dev.epicduels.stats;

import dev.epicduels.model.PlayerStats;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstraction for remote stats backends (Supabase, Firebase, etc.).
 * All methods return CompletableFutures so they never block the main thread.
 */
public interface StatsProvider {

    /**
     * Fetch a player's stats from the remote backend.
     * Returns null inside the future if the player has no remote record.
     */
    CompletableFuture<PlayerStats> fetch(UUID playerId);

    /**
     * Push (upsert) a player's stats to the remote backend.
     */
    CompletableFuture<Void> push(UUID playerId, PlayerStats stats);
}
