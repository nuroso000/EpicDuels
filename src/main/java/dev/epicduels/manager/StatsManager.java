package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.PlayerStats;
import dev.epicduels.stats.FirebaseProvider;
import dev.epicduels.stats.StatsProvider;
import dev.epicduels.stats.SupabaseProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Manages player duel statistics.
 * <p>
 * The local YAML file (stats.yml) always acts as the primary cache.
 * When a remote backend is configured (Supabase or Firebase), stats are
 * refreshed with a bounded cache lifetime. Result events use a durable
 * outbox and idempotent backend transactions; cached totals are never uploaded.
 */
public class StatsManager {

    private final EpicDuels plugin;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final File dataFile;

    // Optional remote backend — null when backend is "local"
    private StatsProvider remoteProvider;

    public StatsManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "stats.yml");
        initRemoteProvider();
        loadStats();
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : new java.util.HashSet<>(pending.values().stream().map(PendingResult::playerId).toList())) flushPending(uuid);
        }, 20L, 600L);
    }

    // ========== Remote provider setup ==========

    private void initRemoteProvider() {
        String backend = plugin.getConfig().getString("stats.backend", "local").toLowerCase();

        switch (backend) {
            case "supabase" -> {
                String url = plugin.getConfig().getString("stats.supabase.url", "");
                String key = plugin.getConfig().getString("stats.supabase.api-key", "");
                String table = plugin.getConfig().getString("stats.supabase.table", "player_stats");
                if (url.isEmpty() || key.isEmpty()) {
                    plugin.getLogger().warning("Supabase backend selected but url/api-key is missing — falling back to local.");
                    return;
                }
                remoteProvider = new SupabaseProvider(url, key, table, plugin.getLogger());
                plugin.getLogger().info("Stats backend: Supabase (" + url + ")");
            }
            case "firebase" -> {
                String dbUrl = plugin.getConfig().getString("stats.firebase.database-url", "");
                String auth = plugin.getConfig().getString("stats.firebase.auth-token", "");
                if (dbUrl.isEmpty() || auth.isEmpty()) {
                    plugin.getLogger().warning("Firebase backend selected but database-url/server auth-token is missing — falling back to local.");
                    return;
                }
                remoteProvider = new FirebaseProvider(dbUrl, auth, plugin.getLogger());
                plugin.getLogger().info("Stats backend: Firebase (" + dbUrl + ")");
            }
            default -> plugin.getLogger().info("Stats backend: local (stats.yml)");
        }
    }

    // ========== Load / Save (local YAML) ==========

    private record PendingResult(UUID id, UUID playerId, boolean win) {}
    private final Map<UUID, PendingResult> pending = new java.util.LinkedHashMap<>();
    private final java.util.Set<UUID> sending = new java.util.HashSet<>();
    private final java.util.Set<UUID> fetching = new java.util.HashSet<>();
    private final Map<UUID, Long> refreshed = new HashMap<>();
    private final Map<UUID, Long> revisions = new HashMap<>();
    private final java.util.List<CompletableFuture<Void>> remoteWrites = new ArrayList<>();

    public void loadStats() {
        stats.clear();
        pending.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        org.bukkit.configuration.ConfigurationSection outbox = config.getConfigurationSection("_pending");
        if (outbox != null) for (String key : outbox.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                pending.put(id, new PendingResult(id, UUID.fromString(outbox.getString(key + ".player")), outbox.getBoolean(key + ".win")));
            } catch (RuntimeException error) { throw new IllegalStateException("Invalid stats outbox: " + key, error); }
        }
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int wins = config.getInt(key + ".wins", 0);
                int losses = config.getInt(key + ".losses", 0);
                stats.put(uuid, new PlayerStats(wins, losses));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    public void saveStats() { persistStats(); }

    private boolean persistStats() {
        return writeStatsFile(serializeStats(), ++snapshotVersion);
    }

    /**
     * Serializes on the main thread (the stats map is not thread-safe) and
     * writes the file asynchronously so wins/losses don't block the tick.
     * Falls back to a synchronous write during shutdown.
     */
    private long snapshotVersion;
    private long writtenVersion;

    private void saveStatsAsync() {
        String data = serializeStats();
        long version = ++snapshotVersion;
        if (plugin.isEnabled()) {
            org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> writeStatsFile(data, version));
        } else writeStatsFile(data, version);
    }

    private String serializeStats() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String key = entry.getKey().toString();
            config.set(key + ".wins", entry.getValue().getWins());
            config.set(key + ".losses", entry.getValue().getLosses());
        }
        for (PendingResult result : pending.values()) {
            config.set("_pending." + result.id() + ".player", result.playerId().toString());
            config.set("_pending." + result.id() + ".win", result.win());
        }
        return config.saveToString();
    }

    private final Object saveLock = new Object();

    private boolean writeStatsFile(String data, long version) {
        synchronized (saveLock) {
            if (version <= writtenVersion) return true;
            java.nio.file.Path temporary = null;
            try {
                temporary = java.nio.file.Files.createTempFile(dataFile.toPath().getParent(), "stats-", ".tmp");
                java.nio.file.Files.writeString(temporary, data);
                java.nio.file.Files.move(temporary, dataFile.toPath(), java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                writtenVersion = version;
                return true;
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
                return false;
            } finally {
                if (temporary != null) try { java.nio.file.Files.deleteIfExists(temporary); } catch (IOException ignored) {}
            }
        }
    }

    // ========== Public API ==========

    /**
     * Get stats for a player. If a remote provider is configured and we
     * the cache lifetime expires, an async fetch is kicked off. The local
     * cache is always returned immediately (may be 0/0 on the very first
     * call until the remote responds).
     */
    public PlayerStats getStats(UUID uuid) {
        PlayerStats local = stats.computeIfAbsent(uuid, ignored -> new PlayerStats(0, 0));
        flushPending(uuid);
        refresh(uuid);
        return local;
    }

    private void refresh(UUID uuid) {
        if (remoteProvider == null || sending.contains(uuid) || fetching.contains(uuid)
                || pending.values().stream().anyMatch(result -> result.playerId().equals(uuid))
                || System.currentTimeMillis() - refreshed.getOrDefault(uuid, 0L) < 30_000) return;
        fetching.add(uuid);
        long revision = revisions.getOrDefault(uuid, 0L);
        remoteProvider.fetch(uuid).whenComplete((remote, error) -> {
            if (!plugin.isEnabled()) return;
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                fetching.remove(uuid);
                if (revision != revisions.getOrDefault(uuid, 0L) || sending.contains(uuid)) return;
                if (error != null) return;
                if (remote != null) {
                    PlayerStats local = stats.computeIfAbsent(uuid, ignored -> new PlayerStats(0, 0));
                    local.setWins(remote.getWins());
                    local.setLosses(remote.getLosses());
                    saveStatsAsync();
                }
                refreshed.put(uuid, System.currentTimeMillis());
            });
        });
    }

    public void addWin(UUID uuid) { recordMatch(List.of(uuid), List.of()); }
    public void addLoss(UUID uuid) { recordMatch(List.of(), List.of(uuid)); }

    public void recordMatch(java.util.Collection<UUID> winners, java.util.Collection<UUID> losers) {
        for (UUID uuid : winners) appendResult(uuid, true);
        for (UUID uuid : losers) appendResult(uuid, false);
        // A complete match and its outbox events share one local snapshot.
        if (remoteProvider == null) saveStatsAsync();
        else {
            saveStats();
            for (UUID uuid : winners) flushPending(uuid);
            for (UUID uuid : losers) flushPending(uuid);
        }
    }

    private void appendResult(UUID uuid, boolean win) {
        PlayerStats local = stats.computeIfAbsent(uuid, ignored -> new PlayerStats(0, 0));
        if (win) local.addWin(); else local.addLoss();
        revisions.merge(uuid, 1L, Long::sum);
        refreshed.remove(uuid);
        if (remoteProvider != null) {
            UUID eventId = UUID.randomUUID();
            pending.put(eventId, new PendingResult(eventId, uuid, win));
        }
    }

    private void flushPending(UUID uuid) {
        if (remoteProvider == null || sending.contains(uuid)) return;
        PendingResult next = pending.values().stream().filter(result -> result.playerId().equals(uuid)).findFirst().orElse(null);
        if (next == null) return;
        if (!persistStats()) return; // Never send an event that is not yet durable.
        sending.add(uuid);
        CompletableFuture<Void> write = remoteProvider.recordResult(next.id(), uuid, next.win());
        remoteWrites.removeIf(CompletableFuture::isDone);
        remoteWrites.add(write);
        write.whenComplete((unused, error) -> {
            if (!plugin.isEnabled()) return; // Unacknowledged disk events are safe to replay after restart.
            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                sending.remove(uuid);
                if (error != null) {
                    plugin.getLogger().warning("Remote result retained for retry: " + next.id());
                    return;
                }
                pending.remove(next.id());
                revisions.merge(uuid, 1L, Long::sum);
                saveStats();
                flushPending(uuid);
                refresh(uuid);
            });
        });
    }

    public void pushAllToRemote() {
        // Never upload absolute cache totals; retain unacknowledged idempotent events on disk.
        saveStats();
        for (UUID uuid : new java.util.HashSet<>(pending.values().stream().map(PendingResult::playerId).toList())) flushPending(uuid);
        try {
            CompletableFuture.allOf(remoteWrites.toArray(new CompletableFuture[0])).get(15, TimeUnit.SECONDS);
        } catch (Exception error) {
            plugin.getLogger().warning("Remote results remain in the durable outbox for retry.");
        }
    }

    // ========== Leaderboard ==========

    /**
     * Calculate a player's score.
     * <p>
     * Formula: {@code score = wins × winrate = wins² / (wins + losses)}, rounded.
     * A player with 0 games has a score of 0.
     */
    public static int calculateScore(PlayerStats stats) {
        if (stats == null) return 0;
        int wins = stats.getWins();
        int losses = stats.getLosses();
        int total = wins + losses;
        if (total == 0) return 0;
        double score = (double) wins * wins / (double) total;
        return (int) Math.round(score);
    }

    /** Snapshot of a leaderboard row — UUID + cached stats + derived score. */
    public static final class LeaderboardEntry {
        public final UUID uuid;
        public final int wins;
        public final int losses;
        public final int score;

        public LeaderboardEntry(UUID uuid, int wins, int losses, int score) {
            this.uuid = uuid;
            this.wins = wins;
            this.losses = losses;
            this.score = score;
        }
    }

    /** Returns the top N players ordered by total wins (desc). */
    public List<LeaderboardEntry> getTopByWins(int limit) {
        return buildLeaderboard(Comparator.<LeaderboardEntry>comparingInt(e -> e.wins).reversed(), limit);
    }

    /** Returns the top N players ordered by score (desc). */
    public List<LeaderboardEntry> getTopByScore(int limit) {
        return buildLeaderboard(Comparator.<LeaderboardEntry>comparingInt(e -> e.score).reversed(), limit);
    }

    private List<LeaderboardEntry> buildLeaderboard(Comparator<LeaderboardEntry> order, int limit) {
        List<LeaderboardEntry> all = new ArrayList<>(stats.size());
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            PlayerStats s = entry.getValue();
            if (s.getWins() == 0 && s.getLosses() == 0) continue;
            all.add(new LeaderboardEntry(entry.getKey(), s.getWins(), s.getLosses(), calculateScore(s)));
        }
        all.sort(order);
        if (all.size() > limit) {
            return new ArrayList<>(all.subList(0, limit));
        }
        return all;
    }
}
