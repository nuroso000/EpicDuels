package dev.epicduels;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import dev.epicduels.gui.*;
import dev.epicduels.gui.MenuType;
import dev.epicduels.listener.GUIListener;
import dev.epicduels.manager.*;
import dev.epicduels.model.*;
import dev.epicduels.stats.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;

/** Focused regressions against production handlers with isolated Paper API doubles. */
public final class BugAuditChecks {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0d;
        if (type == float.class) return 0f;
        return null;
    }
    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
    private static void field(Object target, Class<?> owner, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    private static <T> T allocate(Class<T> type) throws Exception {
        Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return type.cast(((sun.misc.Unsafe) field.get(null)).allocateInstance(type));
    }
    private static EpicDuels plugin(Path dir) throws Exception {
        EpicDuels plugin = allocate(EpicDuels.class);
        field(plugin, JavaPlugin.class, "dataFolder", dir.toFile());
        Logger logger = Logger.getAnonymousLogger(); logger.setLevel(java.util.logging.Level.OFF);
        field(plugin, JavaPlugin.class, "logger", logger);
        field(plugin, JavaPlugin.class, "newConfig", new YamlConfiguration());
        field(plugin, JavaPlugin.class, "isEnabled", true);
        return plugin;
    }
    private static Player player(UUID id, AtomicInteger mutations) {
        PlayerInventory inventory = proxy(PlayerInventory.class, (p,m,a) -> {
            if (m.getName().startsWith("set") || m.getName().equals("clear")) mutations.incrementAndGet();
            return defaultValue(m.getReturnType());
        });
        return proxy(Player.class, (p,m,a) -> switch (m.getName()) {
            case "getUniqueId" -> id;
            case "getInventory" -> inventory;
            case "getName" -> "TestPlayer";
            case "isOnline", "hasPermission" -> true;
            default -> defaultValue(m.getReturnType());
        });
    }
    private static void editorChecks() throws Exception {
        GUIListener listener = new GUIListener(null);
        Method handler = GUIListener.class.getDeclaredMethod("handleKitCustomizeClick", InventoryClickEvent.class, Player.class, MenuHolder.class);
        handler.setAccessible(true);
        Player player = player(UUID.randomUUID(), new AtomicInteger());
        Inventory inventory = proxy(Inventory.class, (p,m,a) -> m.getName().equals("getSize") ? 54 : defaultValue(m.getReturnType()));
        InventoryView view = proxy(InventoryView.class, (p,m,a) -> switch (m.getName()) {
            case "getTopInventory", "getBottomInventory", "getInventory" -> inventory;
            case "getPlayer" -> player;
            case "getType" -> InventoryType.CHEST;
            case "getTitle", "getOriginalTitle" -> "Editor";
            case "convertSlot" -> a[0];
            default -> defaultValue(m.getReturnType());
        });
        for (InventoryAction action : List.of(InventoryAction.DROP_ONE_SLOT, InventoryAction.DROP_ALL_SLOT,
                InventoryAction.DROP_ONE_CURSOR, InventoryAction.DROP_ALL_CURSOR, InventoryAction.HOTBAR_SWAP,
                InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.CLONE_STACK)) {
            InventoryClickEvent event = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 0, ClickType.DROP, action);
            handler.invoke(listener, event, player, new MenuHolder(MenuType.KIT_CUSTOMIZE));
            check(event.isCancelled(), "Blocked editor action " + action);
        }
        InventoryClickEvent outside = new InventoryClickEvent(view, InventoryType.SlotType.OUTSIDE, -999, ClickType.LEFT, InventoryAction.NOTHING);
        handler.invoke(listener, outside, player, new MenuHolder(MenuType.KIT_CUSTOMIZE));
        check(outside.isCancelled(), "Outside editor interaction blocked");
        InventoryClickEvent inside = new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, 3, ClickType.LEFT, InventoryAction.PICKUP_ALL);
        handler.invoke(listener, inside, player, new MenuHolder(MenuType.KIT_CUSTOMIZE));
        check(!inside.isCancelled(), "Legitimate internal rearrangement preserved");
    }
    private static void backupChecks(Path dir) throws Exception {
        EpicDuels plugin = plugin(dir);
        InventoryBackupManager backups = new InventoryBackupManager(plugin);
        UUID id = UUID.randomUUID(); AtomicInteger mutations = new AtomicInteger(); Player player = player(id, mutations);
        Path file = dir.resolve("inventories").resolve(id + ".yml");
        for (String corrupt : List.of("", "contents: invalid\narmor: invalid\noffhand: invalid", "contents: ''\narmor: ''")) {
            Files.writeString(file, corrupt);
            check(!backups.apply(player), "Corrupt snapshot rejected");
            check(!backups.restore(player), "Corrupt restore rejected");
            check(Files.exists(file), "Corrupt snapshot retained");
            check(mutations.get() == 0, "Inventory unmodified on corrupt snapshot");
        }
        check(!backups.backup(player), "Unresolved snapshot cannot be overwritten");
    }
    private static void snapshotChecks(Path dir) throws Exception {
        StatsManager manager = allocate(StatsManager.class);
        field(manager, StatsManager.class, "plugin", plugin(dir));
        field(manager, StatsManager.class, "dataFile", dir.resolve("stats.yml").toFile());
        field(manager, StatsManager.class, "saveLock", new Object());
        Method write = StatsManager.class.getDeclaredMethod("writeStatsFile", String.class, long.class); write.setAccessible(true);
        write.invoke(manager, "new", 2L); write.invoke(manager, "old", 1L);
        check(Files.readString(dir.resolve("stats.yml")).equals("new"), "Old async snapshot cannot overwrite newer result");
        write.invoke(manager, "shutdown", 3L); write.invoke(manager, "late", 2L);
        check(Files.readString(dir.resolve("stats.yml")).equals("shutdown"), "Shutdown snapshot remains final");
    }
    private static void parserChecks() throws Exception {
        for (Class<?> type : List.of(SupabaseProvider.class, FirebaseProvider.class)) {
            Object provider = allocate(type);
            Method parse = type.getDeclaredMethod("parseResponse", String.class); parse.setAccessible(true);
            for (String json : List.of("{\"wins\":12,\"losses\":3}", "{\"wins\" : 12, \"losses\" : 3}", "{\n\"losses\": 3,\n\"wins\": 12\n}")) {
                PlayerStats result = (PlayerStats) parse.invoke(provider, type == SupabaseProvider.class ? "[" + json + "]" : json);
                check(result.getWins() == 12 && result.getLosses() == 3, "JSON whitespace and field order accepted by " + type.getSimpleName());
            }
            check(parse.invoke(provider, type == SupabaseProvider.class ? "[]" : "null") == null, "Missing record preserved");
        }
    }
    private static void remoteChecks() throws Exception {
        AtomicReference<JsonObject> data = new AtomicReference<>(JsonParser.parseString("{\"wins\":100,\"losses\":4}").getAsJsonObject());
        AtomicInteger version = new AtomicInteger(); AtomicInteger conflicts = new AtomicInteger();
        AtomicBoolean forceConflict = new AtomicBoolean(true);
        HttpServer server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/player_stats", exchange -> {
            byte[] response;
            synchronized (data) {
                if (exchange.getRequestMethod().equals("GET")) {
                    exchange.getResponseHeaders().add("ETag", "\"" + version.get() + "\"");
                    response = data.get().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(200, response.length);
                } else if (forceConflict.getAndSet(false) || !Objects.equals(exchange.getRequestHeaders().getFirst("If-Match"), "\"" + version.get() + "\"")) {
                    conflicts.incrementAndGet(); response = "{}".getBytes(); exchange.sendResponseHeaders(412, response.length);
                } else {
                    data.set(JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject());
                    version.incrementAndGet(); response = data.get().toString().getBytes(); exchange.sendResponseHeaders(200, response.length);
                }
            }
            exchange.getResponseBody().write(response); exchange.close();
        });
        server.start();
        try {
            FirebaseProvider a = new FirebaseProvider("http://127.0.0.1:" + server.getAddress().getPort(), "test", Logger.getAnonymousLogger());
            FirebaseProvider b = new FirebaseProvider("http://127.0.0.1:" + server.getAddress().getPort(), "test", Logger.getAnonymousLogger());
            UUID player = UUID.randomUUID(), event = UUID.randomUUID();
            CompletableFuture.allOf(a.recordResult(event, player, true), b.recordResult(UUID.randomUUID(), player, true)).get(15, TimeUnit.SECONDS);
            check(data.get().get("wins").getAsInt() == 102, "Concurrent servers preserve both wins and remote baseline");
            check(conflicts.get() > 0, "HTTP 412 conflict retried successfully");
            a.recordResult(event, player, true).get(15, TimeUnit.SECONDS);
            check(data.get().get("wins").getAsInt() == 102, "Result retry is idempotent");
            b.recordResult(UUID.randomUUID(), player, false).get(15, TimeUnit.SECONDS);
            check(data.get().get("losses").getAsInt() == 5, "Loss increment preserves wins");
            check(a.fetch(player).get(15, TimeUnit.SECONDS).getWins() == 102, "Provider fetch reads committed totals");
        } finally { server.stop(0); }
    }
    private static final Queue<Runnable> tasks = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final AtomicReference<World> loadedWorld = new AtomicReference<>();
    private static Path worlds;
    private static final Map<UUID, Player> online = new HashMap<>();
    private static void installServer(Path dir) throws Exception {
        worlds = dir;
        BukkitTask task = proxy(BukkitTask.class, (p,m,a) -> defaultValue(m.getReturnType()));
        BukkitScheduler scheduler = proxy(BukkitScheduler.class, (p,m,a) -> {
            if ((m.getName().equals("runTask") || m.getName().equals("runTaskAsynchronously")
                    || m.getName().equals("runTaskLater")) && a[1] instanceof Runnable runnable) tasks.add(runnable);
            return task;
        });
        Server server = proxy(Server.class, (p,m,a) -> switch (m.getName()) {
            case "getScheduler" -> scheduler;
            case "getWorldContainer" -> worlds.toFile();
            case "getWorlds" -> List.of(proxy(World.class, (wp,wm,wa) -> defaultValue(wm.getReturnType())));
            case "getWorld" -> loadedWorld.get();
            case "getPlayer" -> online.get(a[0]);
            case "unloadWorld" -> false;
            case "createWorld" -> throw new IllegalStateException("Injected world loading failure");
            case "getLogger" -> Logger.getAnonymousLogger();
            default -> defaultValue(m.getReturnType());
        });
        Field serverField = Bukkit.class.getDeclaredField("server"); serverField.setAccessible(true); serverField.set(null, server);
    }
    private static void drain() { Runnable task; while ((task = tasks.poll()) != null) task.run(); }
    private static void statsCoordinationChecks(Path dir) throws Exception {
        EpicDuels plugin = plugin(dir);
        Files.deleteIfExists(dir.resolve("stats.yml"));
        StatsManager stats = new StatsManager(plugin);
        class Remote implements StatsProvider {
            int fetches; final List<CompletableFuture<PlayerStats>> reads = new ArrayList<>();
            final List<CompletableFuture<Void>> writes = new ArrayList<>();
            public CompletableFuture<PlayerStats> fetch(UUID id) {
                fetches++; var future = new CompletableFuture<PlayerStats>(); reads.add(future); return future;
            }
            public CompletableFuture<Void> recordResult(UUID event, UUID player, boolean win) {
                var future = new CompletableFuture<Void>(); writes.add(future); return future;
            }
        }
        Remote remote = new Remote(); field(stats, StatsManager.class, "remoteProvider", remote);
        UUID player = UUID.randomUUID(); PlayerStats local = stats.getStats(player);
        check(remote.fetches == 1, "First access fetches remote stats");
        stats.addWin(player);
        remote.reads.get(0).complete(new PlayerStats(100, 5)); drain();
        check(local.getWins() == 1, "Stale initialization cannot erase a new result");
        check(remote.writes.size() == 1, "Win recorded as an event during initialization");
        remote.writes.get(0).complete(null); drain();
        check(remote.fetches == 2, "Acknowledged result triggers an authoritative refresh");
        remote.reads.get(1).complete(new PlayerStats(101, 5)); drain();
        check(local.getWins() == 101 && local.getLosses() == 5, "Initial remote progress plus local win preserved");
        check(!Files.readString(dir.resolve("stats.yml")).contains("_pending"), "Acknowledged outbox event removed from disk");
        Map<UUID, Long> refreshed = new HashMap<>(); field(stats, StatsManager.class, "refreshed", refreshed);
        stats.getStats(player);
        check(remote.fetches == 3, "Existing local cache still refreshes remote stats");
        remote.reads.get(2).complete(new PlayerStats(111, 7)); drain();
        check(local.getWins() == 111 && local.getLosses() == 7, "Progress from another server becomes visible");
        stats.addLoss(player); remote.writes.get(1).completeExceptionally(new java.io.IOException("network failure")); drain();
        check(Files.readString(dir.resolve("stats.yml")).contains("_pending"), "Failed network result retained durably");
        stats.getStats(player);
        check(remote.writes.size() == 3, "Pending result retries without replacing remote totals");
    }
    private static void lifecycleChecks(Path dir) throws Exception {
        EpicDuels plugin = plugin(dir);
        ArenaManager arenas = allocate(ArenaManager.class); field(arenas, ArenaManager.class, "plugin", plugin);
        field(plugin, EpicDuels.class, "arenaManager", arenas);
        InventoryBackupManager backups = new InventoryBackupManager(plugin); field(plugin, EpicDuels.class, "inventoryBackupManager", backups);
        World world = proxy(World.class, (p,m,a) -> switch (m.getName()) {
            case "getName" -> "arena_instance_test";
            case "getPlayers", "getEntities" -> List.of();
            default -> defaultValue(m.getReturnType());
        });
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        DuelInstance duel = new DuelInstance(a,b,"test","kit",1); duel.setInstanceWorld(world); duel.setActive(false);
        DuelManager manager = allocate(DuelManager.class); field(manager, DuelManager.class, "plugin", plugin);
        Map<UUID,DuelInstance> matches = new HashMap<>(); matches.put(a,duel); matches.put(b,duel);
        field(manager, DuelManager.class, "activeDuels", matches);
        field(manager, DuelManager.class, "frozenPlayers", new HashSet<>());
        field(manager, DuelManager.class, "originalEntities", new HashMap<>());
        check(manager.isFrozen(a), "Ending duel participant remains protected");
        DuelInstance replacement = new DuelInstance(a, UUID.randomUUID(), "test", "kit",1); matches.put(a, replacement);
        Method finish = DuelManager.class.getDeclaredMethod("finishReturn", DuelInstance.class); finish.setAccessible(true); finish.invoke(manager,duel);
        check(matches.get(a) == replacement, "Old duel cleanup preserves a replacement session");
        check(!matches.containsKey(b), "Old duel cleanup removes its own session"); drain();
        TeamDuelInstance team = new TeamDuelInstance(Set.of(a), Set.of(b), "test", "kit"); team.setInstanceWorld(world); team.setActive(false);
        TeamDuelManager teams = allocate(TeamDuelManager.class); field(teams, TeamDuelManager.class, "plugin", plugin);
        Map<UUID,TeamDuelInstance> teamPlayers = new HashMap<>(); teamPlayers.put(a,team); teamPlayers.put(b,team);
        Map<UUID,TeamDuelInstance> teamIds = new HashMap<>(); teamIds.put(team.getId(),team);
        field(teams, TeamDuelManager.class, "activeByPlayer", teamPlayers); field(teams, TeamDuelManager.class, "activeById", teamIds);
        field(teams, TeamDuelManager.class, "frozen", new HashSet<>()); field(teams, TeamDuelManager.class, "deadSpectators", new HashSet<>());
        check(teams.isFrozen(a), "Ending team participant remains protected");
        team.setActive(true); team.markDead(a);
        check(teams.isFrozen(a), "Eliminated team participant cannot attack");
        TeamDuelInstance replacementTeam = new TeamDuelInstance(Set.of(a),Set.of(UUID.randomUUID()),"test","kit"); teamPlayers.put(a,replacementTeam);
        Method teamFinish = TeamDuelManager.class.getDeclaredMethod("finishReturn", TeamDuelInstance.class); teamFinish.setAccessible(true); teamFinish.invoke(teams,team);
        check(teamPlayers.get(a) == replacementTeam, "Old team cleanup preserves reconnect session");
        check(!teamPlayers.containsKey(b), "Old team cleanup removes its own participant"); drain();
        // Start failure invokes and consumes its completion callback exactly once.
        Map<UUID, java.util.function.BiConsumer<UUID, UUID>> callbacks = new HashMap<>();
        AtomicInteger aborted = new AtomicInteger();
        callbacks.put(replacement.getId(), (winner,loser) -> { check(winner == null && loser == null, "Start abort is explicit"); aborted.incrementAndGet(); });
        field(manager, DuelManager.class, "endCallbacks", callbacks);
        Method abort = DuelManager.class.getDeclaredMethod("abortStart", DuelInstance.class); abort.setAccessible(true);
        abort.invoke(manager,replacement); abort.invoke(manager,replacement);
        check(aborted.get() == 1 && callbacks.isEmpty(), "Start abort callback delivered exactly once"); drain();
        // A cancelled return preserves ownership and recovery state.
        AtomicInteger mutations = new AtomicInteger();
        Player base = player(a, mutations);
        Player blocked = proxy(Player.class, (p,m,args) -> switch (m.getName()) {
            case "getActivePotionEffects" -> List.of();
            case "getMaxHealth" -> 20d;
            case "teleport" -> false;
            default -> m.invoke(base,args);
        });
        online.put(a,blocked); matches.put(a,duel);
        Path backup = dir.resolve("inventories").resolve(a + ".yml"); Files.writeString(backup,"contents: ''");
        finish.invoke(manager,duel);
        check(matches.get(a) == duel && manager.isFrozen(a), "Cancelled return keeps ending protection");
        check(Files.exists(backup) && mutations.get() == 0, "Cancelled return never applies or consumes recovery inventory");
        check(!tasks.isEmpty(), "Cancelled return schedules a retry");
        tasks.clear(); online.clear(); matches.clear(); Files.delete(backup);
        // Shutdown must clean registered ending sessions as well as active ones.
        field(manager, DuelManager.class, "spectators", new HashMap<>());
        matches.put(a,duel); matches.put(b,duel); manager.cleanupAll();
        check(matches.isEmpty(), "Shutdown cleans inactive registered duel sessions"); drain();
        teamPlayers.put(a,team); teamIds.put(team.getId(),team); teams.cleanupAll();
        check(teamPlayers.isEmpty() && teamIds.isEmpty(), "Shutdown cleans inactive registered team sessions"); drain();
        // Production world loading must complete exceptionally after a scheduled createWorld failure.
        Arena arena = new Arena("test"); Path template = dir.resolve(arena.getWorldName()); Files.createDirectories(template);
        CompletableFuture<World> future = arenas.createInstanceWorld(arena,"arena_instance_failure");
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (tasks.isEmpty() && System.nanoTime() < deadline) Thread.sleep(5);
        check(!future.isDone() && !tasks.isEmpty(), "World copy chains to scheduler without blocking the caller");
        drain();
        check(future.isCompletedExceptionally(), "World exception completes the future instead of hanging");
        // Failed unload must retain the loaded world's files.
        Path retained = dir.resolve("arena_instance_test"); Files.createDirectories(retained); Files.writeString(retained.resolve("chunk"),"retained");
        loadedWorld.set(world); arenas.deleteInstanceWorld("arena_instance_test"); drain(); loadedWorld.set(null);
        check(Files.exists(retained.resolve("chunk")), "Failed world unload never deletes loaded data");
    }
    private static void accessChecks(Path dir) throws Exception {
        EpicDuels plugin = plugin(dir);
        GUIListener listener = new GUIListener(plugin);
        Method allowed = GUIListener.class.getDeclaredMethod("mayUseMenu", Player.class, MenuType.class); allowed.setAccessible(true);
        AtomicBoolean permission = new AtomicBoolean(true);
        Player base = player(UUID.randomUUID(), new AtomicInteger());
        Player player = proxy(Player.class, (p,m,args) -> m.getName().equals("hasPermission") ? permission.get() : m.invoke(base,args));
        check((Boolean) allowed.invoke(listener,player,MenuType.MATCHMAKING), "Enabled authorized matchmaking remains available");
        permission.set(false);
        for (MenuType type : List.of(MenuType.MATCHMAKING,MenuType.STATS,MenuType.KIT_CUSTOMIZE,MenuType.DUELS,MenuType.KIT_EDIT,MenuType.PARTY_CONFIRM))
            check(!(Boolean) allowed.invoke(listener,player,type), "Revoked permission blocks stale " + type + " menu");
        permission.set(true); plugin.getConfig().set("features.matchmaking",false);
        check(!(Boolean) allowed.invoke(listener,player,MenuType.MATCHMAKING), "Reload-disabled matchmaking blocks stale menu");
        plugin.getConfig().set("features.parties",false);
        check(!(Boolean) allowed.invoke(listener,player,MenuType.PARTY_CONFIRM), "Reload-disabled parties block stale confirmation");
        QueueManager queue = allocate(QueueManager.class); field(queue,QueueManager.class,"plugin",plugin);
        check(!queue.joinQueue(base.getUniqueId(),"kit"), "Queue manager independently enforces disabled feature");
    }
    private static void survivalRespawnChecks(Path dir) throws Exception {
        EpicDuels plugin = plugin(dir); plugin.getConfig().set("lobby.handle-join", false);
        DuelManager duels = allocate(DuelManager.class); field(duels,DuelManager.class,"activeDuels",new HashMap<>());
        TeamDuelManager teams = allocate(TeamDuelManager.class); field(teams,TeamDuelManager.class,"activeByPlayer",new HashMap<>());
        field(plugin,EpicDuels.class,"duelManager",duels); field(plugin,EpicDuels.class,"teamDuelManager",teams);
        World world = proxy(World.class, (p,m,a) -> m.getName().equals("getName") ? "world" : defaultValue(m.getReturnType()));
        Player base = player(UUID.randomUUID(),new AtomicInteger());
        Player player = proxy(Player.class, (p,m,a) -> m.getName().equals("getWorld") ? world : m.invoke(base,a));
        Location bed = new Location(world, 45,70,45);
        var event = new org.bukkit.event.player.PlayerRespawnEvent(player,bed,true,false);
        new dev.epicduels.listener.PlayerListener(plugin).onPlayerRespawn(event);
        check(event.getRespawnLocation().equals(bed), "Unmanaged survival death preserves its bed respawn");
    }
    private static void consumedChecks(Path dir) throws Exception {
        EpicDuels plugin = plugin(dir); UUID id = UUID.randomUUID(); Player player = player(id,new AtomicInteger());
        InventoryBackupManager backups = new InventoryBackupManager(plugin) {
            @Override public boolean apply(Player target) { return true; }
        };
        Path snapshot = dir.resolve("inventories").resolve(id + ".yml");
        Files.writeString(snapshot,"contents: test");
        check(backups.restore(player), "Valid applied snapshot can be durably consumed");
        check(!backups.hasBackup(id) && !backups.restore(player), "Consumed recovery cannot be replayed");
        Files.writeString(snapshot,"contents: test"); Path consumed = dir.resolve("inventories").resolve(id + ".consumed");
        Files.createDirectories(consumed); Files.writeString(consumed.resolve("blocker"),"x");
        check(!backups.restore(player) && backups.hasBackup(id), "Failed consumption retains protected pending recovery");
    }
    private static void diskFailureChecks(Path dir) throws Exception {
        EpicDuels plugin = plugin(dir); StatsManager manager = new StatsManager(plugin);
        AtomicInteger requests = new AtomicInteger();
        field(manager,StatsManager.class,"remoteProvider",new StatsProvider() {
            public CompletableFuture<PlayerStats> fetch(UUID player) { return CompletableFuture.completedFuture(null); }
            public CompletableFuture<Void> recordResult(UUID event, UUID player, boolean win) { requests.incrementAndGet(); return CompletableFuture.completedFuture(null); }
        });
        field(manager,StatsManager.class,"dataFile",dir.resolve("missing-parent").resolve("stats.yml").toFile());
        UUID winner=UUID.randomUUID(),loser=UUID.randomUUID();
        manager.recordMatch(List.of(winner),List.of(loser));
        check(manager.getStats(winner).getWins()==1 && manager.getStats(loser).getLosses()==1, "Disk failure does not throw or interrupt complete match accounting");
        check(requests.get()==0, "Undurable result is never sent remotely");
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("epicduels-checks-");
        installServer(dir);
        editorChecks(); backupChecks(dir); snapshotChecks(dir); parserChecks();
        statsCoordinationChecks(dir); lifecycleChecks(dir); accessChecks(dir); survivalRespawnChecks(dir); consumedChecks(dir); diskFailureChecks(dir); remoteChecks();
        System.out.println("Bug audit regression checks passed (" + assertions + " assertions).");
    }
}
