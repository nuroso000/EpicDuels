package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.PlayerStats;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class StatsManager {

    private final EpicDuels plugin;
    private final Map<UUID, PlayerStats> stats = new HashMap<>();
    private final File dataFile;

    public StatsManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "stats.yml");
        loadStats();
    }

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

    public PlayerStats getStats(UUID uuid) {
        return stats.computeIfAbsent(uuid, k -> new PlayerStats(0, 0));
    }

    public void addWin(UUID uuid) {
        getStats(uuid).addWin();
        saveStats();
    }

    public void addLoss(UUID uuid) {
        getStats(uuid).addLoss();
        saveStats();
    }
}
