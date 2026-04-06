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

/*
 * ============================================================
 * EpicDuels — Architecture Overview
 * ============================================================
 *
 * This plugin is organized around six manager classes, each
 * responsible for a distinct domain. They are all instantiated
 * in onEnable() and accessed through getter methods on the
 * main plugin class.
 *
 * ArenaManager
 *   Handles arena CRUD: creating void template worlds, saving/
 *   loading arena definitions (spawns, icons, ready state) from
 *   arenas.yml, and copying template worlds into isolated
 *   instance worlds for each duel. Also responsible for
 *   recording original block positions when an instance is
 *   created (so the WorldProtectionListener can tell template
 *   blocks from player-placed ones) and for deleting instance
 *   worlds after duels finish.
 *
 * KitManager
 *   Manages kit CRUD: saving player inventories (contents,
 *   armor, offhand) as Base64-encoded data in kits.yml, plus
 *   per-kit display icons. Provides lookup and listing methods
 *   used by both commands and GUIs.
 *
 * DuelManager
 *   Owns the full duel lifecycle: sending / accepting / denying
 *   challenge requests, starting duels (teleport, kit apply,
 *   countdown, freeze), detecting wins (death or disconnect),
 *   ending duels (stats update, announcements, cleanup).
 *   Also exposes startQueueDuel() for the QueueManager to
 *   initiate matchmade duels without a challenge request.
 *
 * QueueManager
 *   Implements kit-based matchmaking. Players join a queue for
 *   a specific kit; a repeating task checks each queue every
 *   second and pairs the first two players, picking a random
 *   ready arena. A separate task sends action-bar updates
 *   showing queue time. Handles cleanup on disconnect.
 *
 * StatsManager
 *   Simple per-player win/loss tracker backed by stats.yml.
 *   Provides getStats(), addWin(), addLoss() and auto-saves
 *   after every change.
 *
 * GUIManager
 *   Builds and manages all chest-based GUIs: the redesigned
 *   main menu (three-section layout with challenge, stats, and
 *   queue), player selection, kit selection, map selection
 *   (with random-map animation), kit edit/preview, and arena/
 *   kit list views. Tracks per-player GUI state for the
 *   multi-step challenge flow (player -> kit -> map).
 *
 * Listeners
 *   - PlayerListener: join (lobby TP), quit (cleanup), move
 *     freeze, damage cancel during countdown, death handling.
 *   - GUIListener: routes inventory clicks to the correct
 *     handler based on inventory title.
 *   - WorldProtectionListener: protects lobby, allows admin
 *     building in templates, allows player block placement in
 *     instances but prevents breaking original map blocks.
 * ============================================================
 */
public class EpicDuels extends JavaPlugin {

    private ArenaManager arenaManager;
    private KitManager kitManager;
    private DuelManager duelManager;
    private QueueManager queueManager;
    private StatsManager statsManager;
    private GUIManager guiManager;

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialize managers
        arenaManager = new ArenaManager(this);
        kitManager = new KitManager(this);
        statsManager = new StatsManager(this);
        guiManager = new GUIManager(this);
        duelManager = new DuelManager(this);
        queueManager = new QueueManager(this);

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

        // Defer world operations to first tick (cannot create worlds during STARTUP phase)
        Bukkit.getScheduler().runTask(this, () -> {
            setupVoidWorld();
            loadArenaWorlds();
            getLogger().info("Arena worlds loaded.");
        });

        getLogger().info("EpicDuels v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {
        // End all active duels
        if (duelManager != null) {
            duelManager.cleanupAll();
        }

        // Cleanup queue
        if (queueManager != null) {
            queueManager.cleanup();
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
        World defaultWorld = Bukkit.getWorld("world");
        if (defaultWorld != null) {
            File markerFile = new File(getDataFolder(), ".void-setup-complete");
            if (!markerFile.exists()) {
                getDataFolder().mkdirs();
                try {
                    markerFile.createNewFile();
                } catch (Exception e) {
                    getLogger().warning("Could not create marker file");
                }

                defaultWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                defaultWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
                defaultWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
                defaultWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
                defaultWorld.setGameRule(GameRule.DO_FIRE_TICK, false);
                defaultWorld.setDifficulty(Difficulty.PEACEFUL);
                defaultWorld.setTime(6000);
                defaultWorld.setSpawnLocation(0, 100, 0);

                getLogger().info("Default world configured for lobby use.");
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

    public QueueManager getQueueManager() {
        return queueManager;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public GUIManager getGUIManager() {
        return guiManager;
    }
}
