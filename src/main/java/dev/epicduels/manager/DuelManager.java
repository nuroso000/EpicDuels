package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import dev.epicduels.model.DuelInstance;
import dev.epicduels.model.DuelRequest;
import dev.epicduels.model.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class DuelManager {

    private final EpicDuels plugin;
    private final Map<UUID, DuelRequest> outgoingRequests = new ConcurrentHashMap<>();
    // receiver -> (sender -> request), so multiple players can challenge the same target
    private final Map<UUID, Map<UUID, DuelRequest>> incomingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, DuelInstance> activeDuels = new ConcurrentHashMap<>();
    private final Set<UUID> frozenPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<UUID, DuelInstance> spectators = new ConcurrentHashMap<>();
    private final Map<UUID, BiConsumer<UUID, UUID>> endCallbacks = new ConcurrentHashMap<>();
    private BukkitTask expirationTask;
    private BukkitTask timeLimitTask;

    public DuelManager(EpicDuels plugin) {
        this.plugin = plugin;
        startExpirationTask();
        startTimeLimitTask();
    }

    private void startExpirationTask() {
        expirationTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, DuelRequest>> it = outgoingRequests.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, DuelRequest> entry = it.next();
                    DuelRequest request = entry.getValue();
                    if (request.isExpired()) {
                        it.remove();
                        removeIncoming(request.getReceiver(), request.getSender());

                        Player sender = Bukkit.getPlayer(request.getSender());
                        Player receiver = Bukkit.getPlayer(request.getReceiver());
                        if (sender != null) {
                            sender.sendMessage(Component.text("Your duel request has expired.", NamedTextColor.RED));
                        }
                        if (receiver != null) {
                            receiver.sendMessage(Component.text("The duel request has expired.", NamedTextColor.GRAY));
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * True if the player is in any kind of match: 1v1 duel, team duel or tournament.
     */
    public boolean isBusy(UUID playerId) {
        if (isInDuel(playerId)) return true;
        if (plugin.getTeamDuelManager() != null && plugin.getTeamDuelManager().isInTeamDuel(playerId)) return true;
        return plugin.getTournamentManager() != null && plugin.getTournamentManager().isInTournament(playerId);
    }

    public boolean sendRequest(UUID sender, UUID receiver, String arenaName, String kitName) {
        if (outgoingRequests.containsKey(sender)) return false;
        if (isBusy(sender) || isBusy(receiver)) return false;

        DuelRequest request = new DuelRequest(sender, receiver, arenaName, kitName);
        outgoingRequests.put(sender, request);
        incomingRequests.computeIfAbsent(receiver, k -> new ConcurrentHashMap<>()).put(sender, request);
        return true;
    }

    /**
     * All non-expired incoming requests for a player. Expired entries are pruned.
     */
    public List<DuelRequest> getIncomingRequests(UUID receiver) {
        Map<UUID, DuelRequest> map = incomingRequests.get(receiver);
        if (map == null) return List.of();
        List<DuelRequest> valid = new ArrayList<>();
        for (DuelRequest request : map.values()) {
            if (request.isExpired()) {
                map.remove(request.getSender());
                outgoingRequests.remove(request.getSender(), request);
            } else {
                valid.add(request);
            }
        }
        if (map.isEmpty()) incomingRequests.remove(receiver, map);
        return valid;
    }

    /**
     * The single pending incoming request, or null if there are none or several.
     */
    public DuelRequest getIncomingRequest(UUID receiver) {
        List<DuelRequest> valid = getIncomingRequests(receiver);
        return valid.size() == 1 ? valid.get(0) : null;
    }

    public DuelRequest getIncomingRequestFrom(UUID receiver, UUID sender) {
        Map<UUID, DuelRequest> map = incomingRequests.get(receiver);
        if (map == null) return null;
        DuelRequest request = map.get(sender);
        if (request != null && request.isExpired()) {
            removeIncoming(receiver, sender);
            outgoingRequests.remove(sender, request);
            return null;
        }
        return request;
    }

    public DuelRequest getOutgoingRequest(UUID sender) {
        DuelRequest request = outgoingRequests.get(sender);
        if (request != null && request.isExpired()) {
            outgoingRequests.remove(sender);
            removeIncoming(request.getReceiver(), sender);
            return null;
        }
        return request;
    }

    public void cancelRequest(UUID sender) {
        DuelRequest request = outgoingRequests.remove(sender);
        if (request != null) {
            removeIncoming(request.getReceiver(), sender);
        }
    }

    public void denyRequest(UUID receiver, UUID sender) {
        Map<UUID, DuelRequest> map = incomingRequests.get(receiver);
        if (map == null) return;
        DuelRequest request = map.remove(sender);
        if (map.isEmpty()) incomingRequests.remove(receiver, map);
        if (request != null) {
            outgoingRequests.remove(sender, request);
        }
    }

    /**
     * Drop every pending incoming request for a player (quit, duel start, ...).
     */
    public void denyAllIncoming(UUID receiver) {
        Map<UUID, DuelRequest> map = incomingRequests.remove(receiver);
        if (map == null) return;
        for (DuelRequest request : map.values()) {
            outgoingRequests.remove(request.getSender(), request);
        }
    }

    private void removeIncoming(UUID receiver, UUID sender) {
        Map<UUID, DuelRequest> map = incomingRequests.get(receiver);
        if (map != null) {
            map.remove(sender);
            if (map.isEmpty()) incomingRequests.remove(receiver, map);
        }
    }

    public void acceptRequest(UUID receiver, UUID senderId) {
        DuelRequest request = getIncomingRequestFrom(receiver, senderId);
        if (request == null) return;
        denyRequest(receiver, senderId); // consume the request

        Player player1 = Bukkit.getPlayer(request.getSender());
        Player player2 = Bukkit.getPlayer(receiver);
        if (player1 == null || player2 == null) return;

        if (isBusy(player1.getUniqueId()) || isBusy(player2.getUniqueId())) {
            player1.sendMessage(Component.text("Duel could not start: one of you is already in a match.", NamedTextColor.RED));
            player2.sendMessage(Component.text("Duel could not start: one of you is already in a match.", NamedTextColor.RED));
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(request.getArenaName());
        Kit kit = plugin.getKitManager().getKit(request.getKitName());
        if (arena == null || kit == null) {
            player1.sendMessage(Component.text("Duel could not start: arena or kit no longer exists.", NamedTextColor.RED));
            player2.sendMessage(Component.text("Duel could not start: arena or kit no longer exists.", NamedTextColor.RED));
            return;
        }

        startDuel(player1, player2, arena, kit);
    }

    public void startQueueDuel(Player player1, Player player2, String arenaName, String kitName) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (arena == null || kit == null) {
            player1.sendMessage(Component.text("Duel could not start: arena or kit no longer exists.", NamedTextColor.RED));
            player2.sendMessage(Component.text("Duel could not start: arena or kit no longer exists.", NamedTextColor.RED));
            return;
        }
        startDuel(player1, player2, arena, kit);
    }

    public DuelInstance startQueueDuelWithCallback(Player player1, Player player2, String arenaName, String kitName,
                                                    BiConsumer<UUID, UUID> onEnd) {
        Arena arena = plugin.getArenaManager().getArena(arenaName);
        Kit kit = plugin.getKitManager().getKit(kitName);
        if (arena == null || kit == null) {
            player1.sendMessage(Component.text("Duel could not start: arena or kit no longer exists.", NamedTextColor.RED));
            player2.sendMessage(Component.text("Duel could not start: arena or kit no longer exists.", NamedTextColor.RED));
            return null;
        }
        DuelInstance duel = startDuel(player1, player2, arena, kit);
        if (duel != null && onEnd != null) {
            endCallbacks.put(duel.getId(), onEnd);
        }
        return duel;
    }

    private DuelInstance startDuel(Player player1, Player player2, Arena arena, Kit kit) {
        // Guard: never start a duel for someone who is already fighting.
        // Tournament membership is intentionally NOT checked here — tournament
        // matches are themselves started through this method.
        boolean inTeamDuel = plugin.getTeamDuelManager() != null
                && (plugin.getTeamDuelManager().isInTeamDuel(player1.getUniqueId())
                    || plugin.getTeamDuelManager().isInTeamDuel(player2.getUniqueId()));
        if (isInDuel(player1.getUniqueId()) || isInDuel(player2.getUniqueId()) || inTeamDuel) {
            player1.sendMessage(Component.text("Duel could not start: one of you is already in a match.", NamedTextColor.RED));
            player2.sendMessage(Component.text("Duel could not start: one of you is already in a match.", NamedTextColor.RED));
            return null;
        }

        DuelInstance duel = new DuelInstance(player1.getUniqueId(), player2.getUniqueId(), arena.getName(), kit.getName());
        activeDuels.put(player1.getUniqueId(), duel);
        activeDuels.put(player2.getUniqueId(), duel);

        // Safety: ensure neither player is still sitting in a matchmaking queue
        plugin.getQueueManager().removePlayer(player1.getUniqueId());
        plugin.getQueueManager().removePlayer(player2.getUniqueId());

        // Safety: cancel any outgoing/incoming duel requests for both players
        cancelRequest(player1.getUniqueId());
        cancelRequest(player2.getUniqueId());
        denyAllIncoming(player1.getUniqueId());
        denyAllIncoming(player2.getUniqueId());

        player1.sendMessage(Component.text("Preparing duel arena...", NamedTextColor.YELLOW));
        player2.sendMessage(Component.text("Preparing duel arena...", NamedTextColor.YELLOW));

        // Copy and load the arena world, passing the duel instance to record original blocks
        plugin.getArenaManager().createInstanceWorld(arena, duel).thenAccept(world -> {
            if (world == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player1.sendMessage(Component.text("Failed to create duel arena!", NamedTextColor.RED));
                    player2.sendMessage(Component.text("Failed to create duel arena!", NamedTextColor.RED));
                    activeDuels.remove(player1.getUniqueId());
                    activeDuels.remove(player2.getUniqueId());
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                duel.setInstanceWorld(world);
                duel.setActive(true);

                // Validate spawn locations
                if (arena.getSpawn1() == null || arena.getSpawn2() == null) {
                    plugin.getLogger().severe("Arena '" + arena.getName() + "' has null spawn points! spawn1="
                            + arena.getSpawn1() + ", spawn2=" + arena.getSpawn2());
                    player1.sendMessage(Component.text("Arena spawn points are not configured! Please notify an admin.", NamedTextColor.RED));
                    player2.sendMessage(Component.text("Arena spawn points are not configured! Please notify an admin.", NamedTextColor.RED));
                    duel.setActive(false);
                    activeDuels.remove(player1.getUniqueId());
                    activeDuels.remove(player2.getUniqueId());
                    plugin.getArenaManager().deleteInstanceWorld(duel.getInstanceWorldName());
                    return;
                }

                // Calculate spawn locations in the instance world
                Location spawn1 = arena.getSpawn1().clone();
                spawn1.setWorld(world);
                Location spawn2 = arena.getSpawn2().clone();
                spawn2.setWorld(world);

                // Clear and prepare players
                preparePlayer(player1);
                preparePlayer(player2);

                // Teleport players
                player1.teleport(spawn1);
                player2.teleport(spawn2);

                // Apply kit
                applyKit(player1, kit);
                applyKit(player2, kit);

                // Freeze players during countdown
                frozenPlayers.add(player1.getUniqueId());
                frozenPlayers.add(player2.getUniqueId());

                // Countdown
                startCountdown(duel);
            });
        });
        return duel;
    }

    private void startCountdown(DuelInstance duel) {
        new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                Player p1 = Bukkit.getPlayer(duel.getPlayer1());
                Player p2 = Bukkit.getPlayer(duel.getPlayer2());

                if (p1 == null || p2 == null || !duel.isActive()) {
                    cancel();
                    return;
                }

                if (count > 0) {
                    NamedTextColor color = switch (count) {
                        case 5, 4 -> NamedTextColor.RED;
                        case 3, 2 -> NamedTextColor.YELLOW;
                        case 1 -> NamedTextColor.GREEN;
                        default -> NamedTextColor.WHITE;
                    };

                    Title title = Title.title(
                            Component.text(String.valueOf(count), color, TextDecoration.BOLD),
                            Component.text("Get ready!", NamedTextColor.GRAY),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)
                    );
                    p1.showTitle(title);
                    p2.showTitle(title);

                    p1.playSound(p1.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    p2.playSound(p2.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);

                    count--;
                } else {
                    // Fight!
                    Title title = Title.title(
                            Component.text("FIGHT!", NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                    );
                    p1.showTitle(title);
                    p2.showTitle(title);

                    p1.playSound(p1.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                    p2.playSound(p2.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);

                    frozenPlayers.remove(duel.getPlayer1());
                    frozenPlayers.remove(duel.getPlayer2());
                    duel.setCountdownComplete(true);

                    int timeLimit = plugin.getConfig().getInt("duel.time-limit-seconds", 300);
                    if (timeLimit > 0) {
                        duel.setDeadlineMillis(System.currentTimeMillis() + timeLimit * 1000L);
                    }

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ========== Time limit & draws ==========

    private void startTimeLimitTask() {
        timeLimitTask = new BukkitRunnable() {
            @Override
            public void run() {
                Set<UUID> seen = new HashSet<>();
                for (DuelInstance duel : activeDuels.values()) {
                    if (!seen.add(duel.getId())) continue;
                    if (!duel.isActive() || !duel.isCountdownComplete() || duel.getDeadlineMillis() <= 0) continue;

                    long remaining = duel.getDeadlineMillis() - System.currentTimeMillis();
                    if (remaining <= 0) {
                        handleTimeUp(duel);
                    } else {
                        sendTimeActionBar(duel, remaining);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void sendTimeActionBar(DuelInstance duel, long remainingMillis) {
        long seconds = (remainingMillis + 999) / 1000;
        NamedTextColor color = seconds <= 30 ? NamedTextColor.RED
                : seconds <= 60 ? NamedTextColor.YELLOW
                : NamedTextColor.GRAY;
        String prefix = duel.isSuddenDeath() ? "Sudden Death: " : "Time left: ";
        Component bar = Component.text(prefix + formatTime(seconds), color);

        Player p1 = Bukkit.getPlayer(duel.getPlayer1());
        Player p2 = Bukkit.getPlayer(duel.getPlayer2());
        if (p1 != null) p1.sendActionBar(bar);
        if (p2 != null) p2.sendActionBar(bar);
    }

    private static String formatTime(long totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private void handleTimeUp(DuelInstance duel) {
        // Tournament matches always need a winner — resolve per config
        if (endCallbacks.containsKey(duel.getId())) {
            String resolution = plugin.getConfig().getString("duel.tournament-draw", "sudden-death");
            if (resolution.equalsIgnoreCase("sudden-death") && !duel.isSuddenDeath()) {
                startSuddenDeath(duel);
                return;
            }

            UUID winnerId = new Random().nextBoolean() ? duel.getPlayer1() : duel.getPlayer2();
            UUID loserId = duel.getOpponent(winnerId);
            Component msg = Component.text("Time is up — a coin flip decides the match!", NamedTextColor.YELLOW);
            Player p1 = Bukkit.getPlayer(duel.getPlayer1());
            Player p2 = Bukkit.getPlayer(duel.getPlayer2());
            if (p1 != null) p1.sendMessage(msg);
            if (p2 != null) p2.sendMessage(msg);
            endDuel(duel, winnerId, loserId);
            return;
        }

        endDuelDraw(duel);
    }

    private void startSuddenDeath(DuelInstance duel) {
        int extension = plugin.getConfig().getInt("duel.sudden-death-seconds", 60);
        duel.setSuddenDeath(true);
        duel.setDeadlineMillis(System.currentTimeMillis() + extension * 1000L);

        Title title = Title.title(
                Component.text("SUDDEN DEATH", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Next kill wins — " + formatTime(extension) + " on the clock!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        for (UUID id : List.of(duel.getPlayer1(), duel.getPlayer2())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) {
                p.showTitle(title);
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.4f);
            }
        }
    }

    /**
     * Ends a duel with no winner: no stats are recorded and both players are
     * returned to the lobby.
     */
    public void endDuelDraw(DuelInstance duel) {
        if (!duel.isActive()) return;
        duel.setActive(false);

        frozenPlayers.remove(duel.getPlayer1());
        frozenPlayers.remove(duel.getPlayer2());

        Player p1 = Bukkit.getPlayer(duel.getPlayer1());
        Player p2 = Bukkit.getPlayer(duel.getPlayer2());
        String name1 = p1 != null ? p1.getName() : "Unknown";
        String name2 = p2 != null ? p2.getName() : "Unknown";

        Component announcement = Component.text("DUEL ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                .append(Component.text(name1, NamedTextColor.YELLOW))
                .append(Component.text(" vs ", NamedTextColor.GRAY))
                .append(Component.text(name2, NamedTextColor.YELLOW))
                .append(Component.text(" ended in a draw!", NamedTextColor.GRAY));
        Bukkit.broadcast(announcement);

        Title drawTitle = Title.title(
                Component.text("DRAW", NamedTextColor.YELLOW, TextDecoration.BOLD),
                Component.text("Time is up — nobody wins.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        if (p1 != null) p1.showTitle(drawTitle);
        if (p2 != null) p2.showTitle(drawTitle);

        removeSpectatorsForDuel(duel);
        endCallbacks.remove(duel.getId());

        scheduleReturnToLobby(duel, p1, p2);
    }

    public void endDuel(DuelInstance duel, UUID winnerId, UUID loserId) {
        if (!duel.isActive()) return;
        duel.setActive(false);

        frozenPlayers.remove(duel.getPlayer1());
        frozenPlayers.remove(duel.getPlayer2());

        Player winner = Bukkit.getPlayer(winnerId);
        Player loser = Bukkit.getPlayer(loserId);
        String winnerName = winner != null ? winner.getName() : "Unknown";
        String loserName = loser != null ? loser.getName() : "Unknown";

        // Update stats
        plugin.getStatsManager().addWin(winnerId);
        plugin.getStatsManager().addLoss(loserId);

        // Announce
        Component announcement = Component.text("DUEL ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                .append(Component.text(winnerName, NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" defeated ", NamedTextColor.GRAY))
                .append(Component.text(loserName, NamedTextColor.RED))
                .append(Component.text("!", NamedTextColor.GRAY));

        Bukkit.broadcast(announcement);

        if (winner != null) {
            Title winTitle = Title.title(
                    Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
                    Component.text("You won the duel!", NamedTextColor.GREEN),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            );
            winner.showTitle(winTitle);
        }

        if (loser != null) {
            Title loseTitle = Title.title(
                    Component.text("DEFEAT", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("Better luck next time!", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
            );
            loser.showTitle(loseTitle);
        }

        // Return spectators to lobby
        removeSpectatorsForDuel(duel);

        // Fire end callback (e.g. tournament bracket advancement)
        BiConsumer<UUID, UUID> cb = endCallbacks.remove(duel.getId());
        if (cb != null) {
            try {
                cb.accept(winnerId, loserId);
            } catch (Throwable t) {
                plugin.getLogger().warning("Duel end callback threw: " + t.getMessage());
            }
        }

        scheduleReturnToLobby(duel, winner, loser);
    }

    /**
     * Waits 3 seconds, then returns both players to the lobby and deletes the
     * instance world.
     */
    private void scheduleReturnToLobby(DuelInstance duel, Player playerA, Player playerB) {
        String instanceWorldName = duel.getInstanceWorldName();

        new BukkitRunnable() {
            @Override
            public void run() {
                Location lobby = plugin.getLobbyLocation();

                for (Player p : new Player[]{playerA, playerB}) {
                    if (p != null && p.isOnline()) {
                        p.getInventory().clear();
                        p.setHealth(p.getMaxHealth());
                        p.setFoodLevel(20);
                        p.setSaturation(20f);
                        p.setGameMode(GameMode.ADVENTURE);
                        p.teleport(lobby);
                    }
                }

                activeDuels.remove(duel.getPlayer1());
                activeDuels.remove(duel.getPlayer2());

                // Clean up instance world
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getArenaManager().deleteInstanceWorld(instanceWorldName);
                }, 20L);
            }
        }.runTaskLater(plugin, 60L); // 3 seconds
    }

    public void handleDisconnect(UUID playerId) {
        DuelInstance duel = activeDuels.get(playerId);
        if (duel == null || !duel.isActive()) return;

        UUID opponent = duel.getOpponent(playerId);
        endDuel(duel, opponent, playerId);
    }

    /**
     * Called when a duel participant leaves the arena world (e.g. teleported
     * away by another plugin). Counts as a forfeit — the opponent wins.
     */
    public void handleForfeit(UUID playerId) {
        DuelInstance duel = activeDuels.get(playerId);
        if (duel == null || !duel.isActive()) return;

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            player.sendMessage(Component.text("You left the arena — duel forfeited.", NamedTextColor.RED));
        }
        UUID opponent = duel.getOpponent(playerId);
        endDuel(duel, opponent, playerId);
    }

    public boolean isInDuel(UUID playerId) {
        DuelInstance duel = activeDuels.get(playerId);
        return duel != null && duel.isActive();
    }

    public boolean isFrozen(UUID playerId) {
        return frozenPlayers.contains(playerId);
    }

    public DuelInstance getDuel(UUID playerId) {
        return activeDuels.get(playerId);
    }

    public DuelInstance getDuelByWorld(String worldName) {
        for (DuelInstance duel : activeDuels.values()) {
            if (duel.getInstanceWorldName().equals(worldName)) {
                return duel;
            }
        }
        return null;
    }

    public DuelInstance getDuelById(UUID duelId) {
        if (duelId == null) return null;
        for (DuelInstance duel : activeDuels.values()) {
            if (duel.getId().equals(duelId)) return duel;
        }
        return null;
    }

    // ========== Spectator methods ==========

    public boolean addSpectator(Player spectator, DuelInstance duel) {
        if (duel.getInstanceWorld() == null) return false;
        spectators.put(spectator.getUniqueId(), duel);

        // Teleport to the duel arena and set spectator mode
        Player p1 = Bukkit.getPlayer(duel.getPlayer1());
        Location spawnLoc = p1 != null ? p1.getLocation() : duel.getInstanceWorld().getSpawnLocation();
        spectator.setGameMode(GameMode.SPECTATOR);
        spectator.teleport(spawnLoc);
        spectator.sendMessage(Component.text("You are now spectating a duel!", NamedTextColor.GREEN));
        return true;
    }

    public void removeSpectator(UUID spectatorId) {
        DuelInstance duel = spectators.remove(spectatorId);
        if (duel == null) return;
        Player spectator = Bukkit.getPlayer(spectatorId);
        if (spectator != null && spectator.isOnline()) {
            spectator.setGameMode(GameMode.ADVENTURE);
            spectator.teleport(plugin.getLobbyLocation());
            spectator.sendMessage(Component.text("You stopped spectating.", NamedTextColor.YELLOW));
        }
    }

    public boolean isSpectating(UUID playerId) {
        return spectators.containsKey(playerId);
    }

    private void removeSpectatorsForDuel(DuelInstance duel) {
        Iterator<Map.Entry<UUID, DuelInstance>> it = spectators.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DuelInstance> entry = it.next();
            if (entry.getValue().getId().equals(duel.getId())) {
                Player spectator = Bukkit.getPlayer(entry.getKey());
                if (spectator != null && spectator.isOnline()) {
                    spectator.setGameMode(GameMode.ADVENTURE);
                    spectator.teleport(plugin.getLobbyLocation());
                    spectator.sendMessage(Component.text("The duel has ended.", NamedTextColor.GRAY));
                }
                it.remove();
            }
        }
    }

    public void cleanupAll() {
        if (expirationTask != null) {
            expirationTask.cancel();
            expirationTask = null;
        }
        if (timeLimitTask != null) {
            timeLimitTask.cancel();
            timeLimitTask = null;
        }

        for (UUID specId : new HashSet<>(spectators.keySet())) {
            removeSpectator(specId);
        }

        for (DuelInstance duel : new HashSet<>(activeDuels.values())) {
            if (duel.isActive()) {
                duel.setActive(false);
                frozenPlayers.remove(duel.getPlayer1());
                frozenPlayers.remove(duel.getPlayer2());
                activeDuels.remove(duel.getPlayer1());
                activeDuels.remove(duel.getPlayer2());
                if (duel.getInstanceWorld() != null) {
                    plugin.getArenaManager().deleteInstanceWorld(duel.getInstanceWorldName());
                }
            }
        }

        outgoingRequests.clear();
        incomingRequests.clear();
        activeDuels.clear();
        frozenPlayers.clear();
        spectators.clear();
        endCallbacks.clear();
    }

    private void preparePlayer(Player player) {
        player.getInventory().clear();
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setGameMode(GameMode.SURVIVAL);
    }

    private void applyKit(Player player, Kit kit) {
        player.getInventory().setContents(kit.getContents());
        if (kit.getArmorContents() != null) {
            player.getInventory().setArmorContents(kit.getArmorContents());
        }
        if (kit.getOffHand() != null) {
            player.getInventory().setItemInOffHand(kit.getOffHand());
        }
    }
}
