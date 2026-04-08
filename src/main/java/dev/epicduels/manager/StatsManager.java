package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.PlayerStats;
import dev.epicduels.stats.FirebaseProvider;
import dev.epicduels.stats.StatsProvider;
import dev.epicduels.stats.SupabaseProvider;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages player duel statistics.
 * <p>
 * The local YAML file (stats.yml) always acts as the primary cache.
 * When a remote backend is configured (Supabase or Firebase), stats are
 * fetched on first access and pushed after every change, so multiple
 * servers share the same data.
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
                if (dbUrl.isEmpty()) {
                    plugin.getLogger().warning("Firebase backend selected but database-url is missing — falling back to local.");
                    return;
                }
                remoteProvider = new FirebaseProvider(dbUrl, auth, plugin.getLogger());
                plugin.getLogger().info("Stats backend: Firebase (" + dbUrl + ")");
            }
            default -> plugin.getLogger().info("Stats backend: local (stats.yml)");
        }
    }

    // ========== Load / Save (local YAML) ==========

    public void loadStats() {
        stats.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
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

    public void saveStats() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            String key = entry.getKey().toString();
            config.set(key + ".wins", entry.getValue().getWins());
            config.set(key + ".losses", entry.getValue().getLosses());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save stats.yml", e);
        }
    }

    // ========== Public API ==========

    /**
     * Get stats for a player. If a remote provider is configured and we
     * don't have local data yet, an async fetch is kicked off. The local
     * cache is always returned immediately (may be 0/0 on the very first
     * call until the remote responds).
     */
    public PlayerStats getStats(UUID uuid) {
        PlayerStats local = stats.get(uuid);
        if (local != null) return local;

        // First access — create default
        local = new PlayerStats(0, 0);
        stats.put(uuid, local);

        // If remote provider is configured, try to pull data
        if (remoteProvider != null) {
            final PlayerStats placeholder = local;
            remoteProvider.fetch(uuid).thenAccept(remote -> {
                if (remote != null) {
                    // Merge: take the higher value for each field so no data is lost
                    int mergedWins = Math.max(placeholder.getWins(), remote.getWins());
                    int mergedLosses = Math.max(placeholder.getLosses(), remote.getLosses());
                    placeholder.setWins(mergedWins);
                    placeholder.setLosses(mergedLosses);
                    // Persist merged data locally
                    saveStats();
                    plugin.getLogger().info("Synced stats for " + uuid + " from remote (" + mergedWins + "W/" + mergedLosses + "L).");
                }
            });
        }

        return local;
    }

    public void addWin(UUID uuid) {
        getStats(uuid).addWin();
        saveStats();
        pushToRemote(uuid);
    }

    public void addLoss(UUID uuid) {
        getStats(uuid).addLoss();
        saveStats();
        pushToRemote(uuid);
    }

    /**
     * Push a player's stats to the remote backend asynchronously.
     */
    private void pushToRemote(UUID uuid) {
        if (remoteProvider == null) return;
        PlayerStats s = stats.get(uuid);
        if (s == null) return;
        remoteProvider.push(uuid, s);
    }

    /**
     * Push ALL cached stats to the remote (called on shutdown so nothing is lost).
     */
    public void pushAllToRemote() {
        if (remoteProvider == null) return;
        for (Map.Entry<UUID, PlayerStats> entry : stats.entrySet()) {
            remoteProvider.push(entry.getKey(), entry.getValue());
        }
        plugin.getLogger().info("Pushed all stats to remote backend.");
    }
}
