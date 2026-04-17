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
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class HologramManager {

    private static final int TOP = 10;
    private static final long REFRESH_TICKS = 1200L; // 60 seconds
    private static final double LINE_HEIGHT = 0.3D;
    private static final String TAG_KEY = "epicduels_leaderboard";

    public enum Type {
        WINS, SCORE;
        public String key() { return name().toLowerCase(); }
    }

    private final EpicDuels plugin;
    private final File dataFile;
    private final NamespacedKey tagKey;
    private final Map<Type, Location> locations = new EnumMap<>(Type.class);
    private final Map<Type, List<ArmorStand>> liveEntities = new EnumMap<>(Type.class);
    private final Map<UUID, String> nameCache = new HashMap<>();
    private BukkitTask refreshTask;

    public HologramManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "leaderboards.yml");
        this.tagKey = new NamespacedKey(plugin, TAG_KEY);
    }

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
                locations.put(type, new Location(
                        world,
                        section.getDouble("x"),
                        section.getDouble("y"),
                        section.getDouble("z")
                ));
            }
        }

        removeOrphanEntities();

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
        liveEntities.clear();
        nameCache.clear();
    }

    private void startRefreshTask() {
        if (refreshTask != null) refreshTask.cancel();
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Type type : Type.values()) {
                if (locations.containsKey(type)) refresh(type);
            }
        }, REFRESH_TICKS, REFRESH_TICKS);
    }

    public void setHologram(Type type, Location location) {
        locations.put(type, location.clone());
        refresh(type);
        save();
    }

    public boolean removeHologram(Type type) {
        removeEntities(type);
        boolean had = locations.remove(type) != null;
        save();
        return had;
    }

    public boolean hasHologram(Type type) {
        return locations.containsKey(type);
    }

    private void refresh(Type type) {
        Location anchor = locations.get(type);
        if (anchor == null || anchor.getWorld() == null) return;

        List<Component> lines = buildLines(type);
        List<ArmorStand> existing = liveEntities.get(type);

        if (existing != null && !existing.isEmpty() && existing.get(0).isValid()) {
            updateInPlace(type, anchor, lines, existing);
        } else {
            removeEntities(type);
            spawnFresh(type, anchor, lines);
        }
    }

    private void updateInPlace(Type type, Location anchor, List<Component> lines, List<ArmorStand> existing) {
        int needed = lines.size();
        int have = existing.size();

        // Update existing stands
        int toUpdate = Math.min(needed, have);
        for (int i = 0; i < toUpdate; i++) {
            ArmorStand stand = existing.get(i);
            if (!stand.isValid()) {
                removeEntities(type);
                spawnFresh(type, anchor, lines);
                return;
            }
            stand.customName(lines.get(i));
            Location target = anchor.clone().add(0, -(i * LINE_HEIGHT), 0);
            if (stand.getLocation().distanceSquared(target) > 0.01) {
                stand.teleport(target);
            }
        }

        // Remove extras
        if (have > needed) {
            for (int i = needed; i < have; i++) {
                existing.get(i).remove();
            }
            liveEntities.put(type, new ArrayList<>(existing.subList(0, needed)));
        }

        // Spawn missing
        if (needed > have) {
            for (int i = have; i < needed; i++) {
                Location loc = anchor.clone().add(0, -(i * LINE_HEIGHT), 0);
                ArmorStand stand = spawnStand(anchor.getWorld(), loc, type, lines.get(i));
                existing.add(stand);
            }
        }
    }

    private void spawnFresh(Type type, Location anchor, List<Component> lines) {
        List<ArmorStand> created = new ArrayList<>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            Location loc = anchor.clone().add(0, -(i * LINE_HEIGHT), 0);
            created.add(spawnStand(anchor.getWorld(), loc, type, lines.get(i)));
        }
        liveEntities.put(type, created);
    }

    private ArmorStand spawnStand(World world, Location loc, Type type, Component name) {
        ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
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
        stand.customName(name);
        return stand;
    }

    private List<Component> buildLines(Type type) {
        List<Component> lines = new ArrayList<>();

        String title = type == Type.WINS ? "Top Wins" : "Top Score";
        lines.add(Component.text("» " + title + " «", NamedTextColor.GOLD, TextDecoration.BOLD));

        List<StatsManager.LeaderboardEntry> top = type == Type.WINS
                ? plugin.getStatsManager().getTopByWins(TOP)
                : plugin.getStatsManager().getTopByScore(TOP);

        if (top.isEmpty()) {
            lines.add(Component.text("No data yet", NamedTextColor.GRAY));
            return lines;
        }

        for (int i = 0; i < top.size(); i++) {
            StatsManager.LeaderboardEntry entry = top.get(i);
            String name = resolvePlayerName(entry.uuid);
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

    private String resolvePlayerName(UUID uuid) {
        String cached = nameCache.get(uuid);
        if (cached != null) return cached;

        OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
        String name = op.getName() != null ? op.getName() : uuid.toString().substring(0, 8);
        nameCache.put(uuid, name);
        return name;
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
        List<ArmorStand> entities = liveEntities.remove(type);
        if (entities == null) return;
        for (ArmorStand stand : entities) {
            if (stand.isValid()) stand.remove();
        }
    }

    private void removeOrphanEntities() {
        for (Type type : Type.values()) {
            Location loc = locations.get(type);
            if (loc == null || loc.getWorld() == null) continue;
            Collection<ArmorStand> stands = loc.getWorld().getEntitiesByClass(ArmorStand.class);
            for (ArmorStand stand : stands) {
                if (stand.getPersistentDataContainer().has(tagKey, PersistentDataType.STRING)) {
                    stand.remove();
                }
            }
        }
    }
}
