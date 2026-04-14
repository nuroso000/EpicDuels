package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Manages leaderboard holograms made of stacked invisible ArmorStands.
 * <p>
 * Two holograms are supported: one ordered by {@link Type#WINS} and one by
 * {@link Type#SCORE}. Each hologram occupies a single configured location
 * and is periodically refreshed (every 10 seconds) from the StatsManager.
 * Positions persist to {@code leaderboards.yml}.
 */
public class HologramManager {

    /** Number of top entries shown per hologram. */
    private static final int TOP = 10;
    /** Refresh interval in ticks (10 seconds). */
    private static final long REFRESH_TICKS = 200L;
    /** Vertical spacing between ArmorStand lines. */
    private static final double LINE_HEIGHT = 0.28D;
    /** Marker tag stored in ArmorStand persistent data container. */
    private static final String TAG_KEY = "epicduels_leaderboard";

    public enum Type {
        WINS, SCORE;

        public String key() { return name().toLowerCase(); }
    }

    private final EpicDuels plugin;
    private final File dataFile;
    private final NamespacedKey tagKey;
    private final Map<Type, Location> locations = new EnumMap<>(Type.class);
    private final Map<Type, List<UUID>> liveEntities = new EnumMap<>(Type.class);
    private BukkitTask refreshTask;

    public HologramManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "leaderboards.yml");
        this.tagKey = new NamespacedKey(plugin, TAG_KEY);
    }

    // ========== Lifecycle ==========

    public void load() {
        locations.clear();
        if (dataFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            for (Type type : Type.values()) {
                ConfigurationSection section = config.getConfigurationSection(type.key());
                if (section == null) continue;
                String worldName = section.getString("world");
                if (worldName == null) continue;
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().warning("Leaderboard hologram world '" + worldName + "' not loaded — hologram skipped.");
                    continue;
                }
                Location loc = new Location(
                        world,
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z")
                );
                locations.put(type, loc);
            }
        }

        // Remove any leftover hologram entities from prior runs so we start clean
        removeOrphanEntities();

        // Spawn the holograms at their configured locations
        for (Type type : locations.keySet()) {
            refresh(type);
        }

        startRefreshTask();
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<Type, Location> entry : locations.entrySet()) {
            String key = entry.getKey().key();
            Location loc = entry.getValue();
            config.set(key + ".world", loc.getWorld().getName());
            config.set(key + ".x", loc.getX());
            config.set(key + ".y", loc.getY());
            config.set(key + ".z", loc.getZ());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save leaderboards.yml", e);
        }
    }

    public void shutdown() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }
        for (Type type : Type.values()) {
            removeEntities(type);
        }
    }

    private void startRefreshTask() {
        if (refreshTask != null) refreshTask.cancel();
        refreshTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Type type : Type.values()) {
                    if (locations.containsKey(type)) refresh(type);
                }
            }
        }.runTaskTimer(plugin, REFRESH_TICKS, REFRESH_TICKS);
    }

    // ========== Public API ==========

    /** Create or move a hologram to the given location. Saves config. */
    public void setHologram(Type type, Location location) {
        locations.put(type, location.clone());
        refresh(type);
        save();
    }

    /** Remove a hologram (entities + persisted location). */
    public boolean removeHologram(Type type) {
        removeEntities(type);
        boolean had = locations.remove(type) != null;
        save();
        return had;
    }

    public boolean hasHologram(Type type) {
        return locations.containsKey(type);
    }

    // ========== Rendering ==========

    private void refresh(Type type) {
        Location anchor = locations.get(type);
        if (anchor == null || anchor.getWorld() == null) return;

        List<Component> lines = buildLines(type);

        // Remove old entities first (simpler than in-place editing)
        removeEntities(type);

        List<UUID> created = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Location line = anchor.clone().add(0, -(i * LINE_HEIGHT), 0);
            ArmorStand stand = anchor.getWorld().spawn(line, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setGravity(false);
                as.setMarker(true);
                as.setSmall(true);
                as.setCustomNameVisible(true);
                as.setInvulnerable(true);
                as.setPersistent(false);
                as.setSilent(true);
                as.getPersistentDataContainer().set(tagKey, PersistentDataType.STRING, type.key());
            });
            stand.customName(lines.get(i));
            created.add(stand.getUniqueId());
        }
        liveEntities.put(type, created);
    }

    private List<Component> buildLines(Type type) {
        List<Component> lines = new ArrayList<>();

        String title = type == Type.WINS ? "Top Wins" : "Top Score";
        lines.add(Component.text("» " + title + " «", NamedTextColor.GOLD, TextDecoration.BOLD));
        lines.add(Component.text(" "));

        List<StatsManager.LeaderboardEntry> top = type == Type.WINS
                ? plugin.getStatsManager().getTopByWins(TOP)
                : plugin.getStatsManager().getTopByScore(TOP);

        if (top.isEmpty()) {
            lines.add(Component.text("No data yet", NamedTextColor.GRAY));
            return lines;
        }

        for (int i = 0; i < top.size(); i++) {
            StatsManager.LeaderboardEntry entry = top.get(i);
            OfflinePlayer op = Bukkit.getOfflinePlayer(entry.uuid);
            String name = op.getName() != null ? op.getName() : entry.uuid.toString().substring(0, 8);
            int value = type == Type.WINS ? entry.wins : entry.score;
            NamedTextColor rankColor = rankColor(i + 1);

            lines.add(Component.text()
                    .append(Component.text("#" + (i + 1) + " ", rankColor, TextDecoration.BOLD))
                    .append(Component.text(name, NamedTextColor.WHITE))
                    .append(Component.text(" — ", NamedTextColor.GRAY))
                    .append(Component.text(String.valueOf(value), NamedTextColor.YELLOW))
                    .build());
        }
        return lines;
    }

    private NamedTextColor rankColor(int rank) {
        return switch (rank) {
            case 1 -> NamedTextColor.GOLD;
            case 2 -> NamedTextColor.GRAY;
            case 3 -> NamedTextColor.RED;
            default -> NamedTextColor.WHITE;
        };
    }

    private void removeEntities(Type type) {
        List<UUID> ids = liveEntities.remove(type);
        if (ids == null) return;
        for (UUID id : ids) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) entity.remove();
        }
    }

    /**
     * Remove any leftover tagged ArmorStands in loaded worlds. Called on load
     * so a server crash or reload never leaves orphan holograms around.
     */
    private void removeOrphanEntities() {
        for (World world : Bukkit.getWorlds()) {
            Collection<ArmorStand> stands = world.getEntitiesByClass(ArmorStand.class);
            for (ArmorStand stand : stands) {
                if (stand.getPersistentDataContainer().has(tagKey, PersistentDataType.STRING)) {
                    stand.remove();
                }
            }
        }
    }
}
