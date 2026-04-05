package dev.epicduels;

import dev.epicduels.command.DuelCommand;
import dev.epicduels.command.DuelTabCompleter;
import dev.epicduels.listener.GUIListener;
import dev.epicduels.listener.PlayerListener;
import dev.epicduels.listener.WorldProtectionListener;
import dev.epicduels.manager.*;
import dev.epicduels.world.VoidWorldGenerator;
import org.bukkit.*;
import org.bukkit.command.PluginCommand;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class EpicDuels extends JavaPlugin {

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private DuelManager duelManager;
    private StatsManager statsManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Setup void world on first start
        setupVoidWorld();

        // Initialize managers
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        statsManager = new StatsManager(this);
        guiManager = new GUIManager(this);
        duelManager = new DuelManager(this);

        // Register commands
        PluginCommand duelCmd = getCommand("duel");
        if (duelCmd != null) {
            duelCmd.setExecutor(new DuelCommand(this));
            duelCmd.setTabCompleter(new DuelTabCompleter(this));
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldProtectionListener(this), this);

        // Load existing arena template worlds
        loadArenaWorlds();

        getLogger().info("EpicDuels v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // End all active duels
        if (duelManager != null) {
            duelManager.cleanupAll();
        }

        // Save all data
        if (arenaManager != null) arenaManager.saveArenas();
        if (kitManager != null) kitManager.saveKits();
        if (statsManager != null) statsManager.saveStats();

        getLogger().info("EpicDuels disabled!");
    }

    @Override
    public @Nullable ChunkGenerator getDefaultWorldGenerator(@NotNull String worldName, @Nullable String id) {
        return new VoidWorldGenerator();
    }

    private void setupVoidWorld() {
        // Check if this is first start by checking if world folder has region data
        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld != null) {
            File regionDir = new File(Bukkit.getWorldContainer(), "world/region");
            // If region files exist and no marker file, this might be first run
            File markerFile = new File(getDataFolder(), ".void-setup-complete");
            if (!markerFile.exists()) {
                getDataFolder().mkdirs();
                try {
                    markerFile.createNewFile();
                } catch (Exception e) {
                    getLogger().warning("Could not create marker file");
                }

                // Configure the default world as void-like
                defaultWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                defaultWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                defaultWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                defaultWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                defaultWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
                defaultWorld.setDifficulty(Difficulty.PEACEFUL);
                defaultWorld.setTime(6000);
                defaultWorld.setSpawnLocation(0, 100, 0);

                getLogger().info("Default world configured for lobby use. Use VoidWorldGenerator in server.properties for true void.");
                getLogger().info("Set level-type=flat and generator-settings in bukkit.yml for void generation.");
            }
        }
    }

    private void loadArenaWorlds() {
        File worldContainer = Bukkit.getWorldContainer();
        File[] dirs = worldContainer.listFiles(File::isDirectory);
        if (dirs == null) return;

        for (File dir : dirs) {
            if (dir.getName().startsWith("arena_template_")) {
                String worldName = dir.getName();
                if (Bukkit.getWorld(worldName) == null) {
                    WorldCreator creator = new WorldCreator(worldName);
                    creator.generator(new VoidWorldGenerator());
                    creator.type(WorldType.FLAT);
                    creator.generateStructures(false);
                    World world = Bukkit.createWorld(creator);
                    if (world != null) {
                        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                        getLogger().info("Loaded arena world: " + worldName);
                    }
                }
            }
        }
    }

    // --- Location helpers ---

    public Location getLobbyLocation() {
        String worldName = getConfig().getString("lobby.world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) world = Bukkit.getWorlds().get(0);
        return new Location(
                world,
                getConfig().getDouble("lobby.x", 0),
                getConfig().getDouble("lobby.y", 100),
                getConfig().getDouble("lobby.z", 0),
                (float) getConfig().getDouble("lobby.yaw", 0),
                (float) getConfig().getDouble("lobby.pitch", 0)
        );
    }

    public void setLobbyLocation(Location loc) {
        getConfig().set("lobby.world", loc.getWorld().getName());
        getConfig().set("lobby.x", loc.getX());
        getConfig().set("lobby.y", loc.getY());
        getConfig().set("lobby.z", loc.getZ());
        getConfig().set("lobby.yaw", (double) loc.getYaw());
        getConfig().set("lobby.pitch", (double) loc.getPitch());
        saveConfig();
    }

    public void setLobbyQueueSpawn(int number, Location loc) {
        String key = "lobby-spawn" + number;
        getConfig().set(key + ".world", loc.getWorld().getName());
        getConfig().set(key + ".x", loc.getX());
        getConfig().set(key + ".y", loc.getY());
        getConfig().set(key + ".z", loc.getZ());
        getConfig().set(key + ".yaw", (double) loc.getYaw());
        getConfig().set(key + ".pitch", (double) loc.getPitch());
        saveConfig();
    }

    // --- Manager getters ---

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }
}
