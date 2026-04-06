package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import dev.epicduels.model.DuelInstance;
import dev.epicduels.world.VoidWorldGenerator;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ArenaManager {

    private final EpicDuels plugin;
    private final Map<String, Arena> arenas = new HashMap<>();
    private final File dataFile;

    public ArenaManager(EpicDuels plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "arenas.yml");
        loadArenas();
    }

    public void loadArenas() {
        arenas.clear();
        if (!dataFile.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        for (String name : config.getKeys(false)) {
            Arena arena = new Arena(name);
            if (config.contains(name + ".spawn1")) {
                arena.setSpawn1(deserializeLocation(config, name + ".spawn1"));
            }
            if (config.contains(name + ".spawn2")) {
                arena.setSpawn2(deserializeLocation(config, name + ".spawn2"));
            }
            arena.setReady(config.getBoolean(name + ".ready", false));
            String iconStr = config.getString(name + ".icon");
            if (iconStr != null) {
                try {
                    arena.setIcon(Material.valueOf(iconStr));
                } catch (IllegalArgumentException ignored) {}
            }
            arenas.put(name.toLowerCase(), arena);
        }
    }

    public void saveArenas() {
        YamlConfiguration config = new YamlConfiguration();
        for (Arena arena : arenas.values()) {
            String name = arena.getName();
            if (arena.getSpawn1() != null) {
                serializeLocation(config, name + ".spawn1", arena.getSpawn1());
            }
            if (arena.getSpawn2() != null) {
                serializeLocation(config, name + ".spawn2", arena.getSpawn2());
            }
            config.set(name + ".ready", arena.isReady());
            if (arena.getIcon() != null) {
                config.set(name + ".icon", arena.getIcon().name());
            }
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save arenas.yml", e);
        }
    }

    public Arena createArena(String name) {
        String key = name.toLowerCase();
        if (arenas.containsKey(key)) return null;

        Arena arena = new Arena(name);
        arenas.put(key, arena);

        // Create void world for this arena
        WorldCreator creator = new WorldCreator(arena.getWorldName());
        creator.generator(new VoidWorldGenerator());
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        World world = Bukkit.createWorld(creator);
        if (world != null) {
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            world.setTime(6000);
            world.setDifficulty(Difficulty.NORMAL);
        }

        saveArenas();
        return arena;
    }

    public boolean deleteArena(String name) {
        String key = name.toLowerCase();
        Arena arena = arenas.remove(key);
        if (arena == null) return false;

        World world = Bukkit.getWorld(arena.getWorldName());
        if (world != null) {
            World fallback = Bukkit.getWorlds().get(0);
            for (Player p : world.getPlayers()) {
                p.teleport(fallback.getSpawnLocation());
            }
            Bukkit.unloadWorld(world, false);
        }

        deleteWorldFolder(new File(Bukkit.getWorldContainer(), arena.getWorldName()));
        saveArenas();
        return true;
    }

    public Arena getArena(String name) {
        return arenas.get(name.toLowerCase());
    }

    public Collection<Arena> getAllArenas() {
        return arenas.values();
    }

    public List<Arena> getReadyArenas() {
        List<Arena> ready = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isReady()) ready.add(arena);
        }
        return ready;
    }

    public List<String> getReadyArenaNames() {
        List<String> names = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.isReady()) names.add(arena.getName());
        }
        return names;
    }

    public CompletableFuture<World> createInstanceWorld(Arena arena, DuelInstance duelInstance) {
        return CompletableFuture.supplyAsync(() -> {
            String templateWorldName = arena.getWorldName();
            String instanceName = duelInstance.getInstanceWorldName();

            File templateDir = new File(Bukkit.getWorldContainer(), templateWorldName);
            File instanceDir = new File(Bukkit.getWorldContainer(), instanceName);

            try {
                copyDirectory(templateDir.toPath(), instanceDir.toPath());
                // Remove uid.dat so Bukkit creates a new one
                File uidDat = new File(instanceDir, "uid.dat");
                if (uidDat.exists()) uidDat.delete();
                File sessionLock = new File(instanceDir, "session.lock");
                if (sessionLock.exists()) sessionLock.delete();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to copy arena world", e);
                return null;
            }

            return instanceName;
        }).thenApply(instanceName -> {
            if (instanceName == null) return null;

            CompletableFuture<World> worldFuture = new CompletableFuture<>();
            Bukkit.getScheduler().runTask(plugin, () -> {
                WorldCreator creator = new WorldCreator((String) instanceName);
                creator.generator(new VoidWorldGenerator());
                creator.type(WorldType.FLAT);
                World world = Bukkit.createWorld(creator);
                if (world != null) {
                    world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                    world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                    world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                    world.setAutoSave(false);

                    // Record all existing (non-air) blocks as original template blocks
                    recordOriginalBlocks(world, duelInstance);
                }
                worldFuture.complete(world);
            });

            try {
                return worldFuture.get();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load instance world", e);
                return null;
            }
        });
    }

    private void recordOriginalBlocks(World world, DuelInstance duelInstance) {
        for (Chunk chunk : world.getLoadedChunks()) {
            int baseX = chunk.getX() << 4;
            int baseZ = chunk.getZ() << 4;
            int minY = world.getMinHeight();
            int maxY = world.getMaxHeight();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minY; y < maxY; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        if (block.getType() != Material.AIR) {
                            duelInstance.recordOriginalBlock(baseX + x, y, baseZ + z);
                        }
                    }
                }
            }
        }
    }

    public void deleteInstanceWorld(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            World fallback = Bukkit.getWorlds().get(0);
            for (Player p : world.getPlayers()) {
                p.teleport(plugin.getLobbyLocation());
            }
            boolean unloaded = Bukkit.unloadWorld(world, false);
            if (!unloaded) {
                plugin.getLogger().warning("Failed to unload instance world: " + worldName);
            }
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File folder = new File(Bukkit.getWorldContainer(), worldName);
            if (!folder.exists()) return;
            try {
                Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete instance world folder: " + worldName, e);
            }
        });
    }

    public void ensureArenaWorldLoaded(Arena arena) {
        if (Bukkit.getWorld(arena.getWorldName()) == null) {
            WorldCreator creator = new WorldCreator(arena.getWorldName());
            creator.generator(new VoidWorldGenerator());
            creator.type(WorldType.FLAT);
            creator.generateStructures(false);
            World world = Bukkit.createWorld(creator);
            if (world != null) {
                world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            }
        }
    }

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void deleteWorldFolder(File folder) {
        if (!folder.exists()) return;
        try {
            Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // Best effort
        }
    }

    private void serializeLocation(YamlConfiguration config, String path, Location loc) {
        config.set(path + ".world", loc.getWorld().getName());
        config.set(path + ".x", loc.getX());
        config.set(path + ".y", loc.getY());
        config.set(path + ".z", loc.getZ());
        config.set(path + ".yaw", (double) loc.getYaw());
        config.set(path + ".pitch", (double) loc.getPitch());
    }

    private Location deserializeLocation(YamlConfiguration config, String path) {
        String worldName = config.getString(path + ".world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(
                world,
                config.getDouble(path + ".x"),
                config.getDouble(path + ".y"),
                config.getDouble(path + ".z"),
                (float) config.getDouble(path + ".yaw"),
                (float) config.getDouble(path + ".pitch")
        );
    }
}
