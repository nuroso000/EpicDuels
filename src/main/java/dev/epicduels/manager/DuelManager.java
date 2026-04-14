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

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DuelManager {

    private final EpicDuels plugin;
    private final Map<UUID, DuelRequest> outgoingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, DuelRequest> incomingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, DuelInstance> activeDuels = new ConcurrentHashMap<>();
    private final Set<UUID> frozenPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    // Spectator UUID -> the DuelInstance they are watching
    private final Map<UUID, DuelInstance> spectators = new ConcurrentHashMap<>();

    public DuelManager(EpicDuels plugin) {
        this.plugin = plugin;
        startExpirationTask();
    }

    private void startExpirationTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, DuelRequest>> it = outgoingRequests.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, DuelRequest> entry = it.next();
                    DuelRequest request = entry.getValue();
                    if (request.isExpired()) {
                        it.remove();
                        incomingRequests.remove(request.getReceiver());

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

    public boolean sendRequest(UUID sender, UUID receiver, String arenaName, String kitName) {
        if (outgoingRequests.containsKey(sender)) return false;
        if (isInDuel(sender) || isInDuel(receiver)) return false;

        DuelRequest request = new DuelRequest(sender, receiver, arenaName, kitName);
        outgoingRequests.put(sender, request);
        incomingRequests.put(receiver, request);
        return true;
    }

    public DuelRequest getIncomingRequest(UUID receiver) {
        DuelRequest request = incomingRequests.get(receiver);
        if (request != null && request.isExpired()) {
            incomingRequests.remove(receiver);
            outgoingRequests.remove(request.getSender());
            return null;
        }
        return request;
    }

    public DuelRequest getIncomingRequestFrom(UUID receiver, UUID sender) {
        DuelRequest request = incomingRequests.get(receiver);
        if (request != null && request.getSender().equals(sender) && !request.isExpired()) {
            return request;
        }
        return null;
    }

    public DuelRequest getOutgoingRequest(UUID sender) {
        DuelRequest request = outgoingRequests.get(sender);
        if (request != null && request.isExpired()) {
            outgoingRequests.remove(sender);
            incomingRequests.remove(request.getReceiver());
            return null;
        }
        return request;
    }

    public void cancelRequest(UUID sender) {
        DuelRequest request = outgoingRequests.remove(sender);
        if (request != null) {
            incomingRequests.remove(request.getReceiver());
        }
    }

    public void denyRequest(UUID receiver) {
        DuelRequest request = incomingRequests.remove(receiver);
        if (request != null) {
            outgoingRequests.remove(request.getSender());
        }
    }

    public void acceptRequest(UUID receiver) {
        DuelRequest request = incomingRequests.remove(receiver);
        if (request == null) return;
        outgoingRequests.remove(request.getSender());

        Player player1 = Bukkit.getPlayer(request.getSender());
        Player player2 = Bukkit.getPlayer(receiver);
        if (player1 == null || player2 == null) return;

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

    private void startDuel(Player player1, Player player2, Arena arena, Kit kit) {
        DuelInstance duel = new DuelInstance(player1.getUniqueId(), player2.getUniqueId(), arena.getName(), kit.getName());
        activeDuels.put(player1.getUniqueId(), duel);
        activeDuels.put(player2.getUniqueId(), duel);

        // Safety: ensure neither player is still sitting in a matchmaking queue
        plugin.getQueueManager().removePlayer(player1.getUniqueId());
        plugin.getQueueManager().removePlayer(player2.getUniqueId());

        // Safety: cancel any outgoing/incoming duel requests for both players
        cancelRequest(player1.getUniqueId());
        cancelRequest(player2.getUniqueId());
        denyRequest(player1.getUniqueId());
        denyRequest(player2.getUniqueId());

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

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
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

        String instanceWorldName = duel.getInstanceWorldName();

        // Wait 3 seconds then teleport and clean up
        new BukkitRunnable() {
            @Override
            public void run() {
                Location lobby = plugin.getLobbyLocation();

                if (winner != null && winner.isOnline()) {
                    winner.getInventory().clear();
                    winner.setHealth(winner.getMaxHealth());
                    winner.setFoodLevel(20);
                    winner.setSaturation(20f);
                    winner.teleport(lobby);
                }
                if (loser != null && loser.isOnline()) {
                    loser.getInventory().clear();
                    loser.setHealth(loser.getMaxHealth());
                    loser.setFoodLevel(20);
                    loser.setSaturation(20f);
                    loser.teleport(lobby);
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
            spectator.setGameMode(GameMode.SURVIVAL);
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
                    spectator.setGameMode(GameMode.SURVIVAL);
                    spectator.teleport(plugin.getLobbyLocation());
                    spectator.sendMessage(Component.text("The duel has ended.", NamedTextColor.GRAY));
                }
                it.remove();
            }
        }
    }

    public void cleanupAll() {
        // Return all spectators to lobby
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
