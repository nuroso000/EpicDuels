package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import dev.epicduels.model.Kit;
import dev.epicduels.model.TeamDuelInstance;
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

public class TeamDuelManager {

    private final EpicDuels plugin;
    private final Map<UUID, TeamDuelInstance> activeByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, TeamDuelInstance> activeById = new ConcurrentHashMap<>();
    // Players who died but their team isn't wiped yet -> kept as spectators inside the arena
    private final Set<UUID> deadSpectators = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<UUID> frozen = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public TeamDuelManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    public boolean startTeamDuel(List<UUID> partyMembers, dev.epicduels.model.TeamSize size, Arena arena, Kit kit) {
        if (partyMembers.size() < size.getTotalPlayers()) return false;

        // Take the first N players, shuffle them for fair team split
        List<UUID> participants = new ArrayList<>(partyMembers.subList(0, size.getTotalPlayers()));
        Collections.shuffle(participants);

        Set<UUID> teamA = new LinkedHashSet<>(participants.subList(0, size.getPlayersPerTeam()));
        Set<UUID> teamB = new LinkedHashSet<>(participants.subList(size.getPlayersPerTeam(), size.getTotalPlayers()));

        TeamDuelInstance instance = new TeamDuelInstance(teamA, teamB, arena.getName(), kit.getName());

        for (UUID id : instance.getAllParticipants()) {
            activeByPlayer.put(id, instance);
            // Safety: drop from queue/requests
            plugin.getQueueManager().removePlayer(id);
            plugin.getDuelManager().cancelRequest(id);
            plugin.getDuelManager().denyRequest(id);
        }
        activeById.put(instance.getId(), instance);

        for (UUID id : instance.getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(Component.text("Preparing team duel arena...", NamedTextColor.YELLOW));
        }

        plugin.getArenaManager().createInstanceWorld(arena, instance.getInstanceWorldName()).thenAccept(world -> {
            if (world == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    failStart(instance, "Failed to create team duel arena!");
                });
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                instance.setInstanceWorld(world);
                instance.setActive(true);

                if (arena.getSpawn1() == null || arena.getSpawn2() == null) {
                    failStart(instance, "Arena spawn points are not configured!");
                    return;
                }

                Location base1 = arena.getSpawn1().clone();
                base1.setWorld(world);
                Location base2 = arena.getSpawn2().clone();
                base2.setWorld(world);

                int idxA = 0;
                for (UUID id : instance.getTeamA()) {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) continue;
                    preparePlayer(p);
                    p.teleport(spawnAround(base1, idxA, instance.getTeamA().size()));
                    applyKit(p, kit);
                    frozen.add(id);
                    idxA++;
                }
                int idxB = 0;
                for (UUID id : instance.getTeamB()) {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) continue;
                    preparePlayer(p);
                    p.teleport(spawnAround(base2, idxB, instance.getTeamB().size()));
                    applyKit(p, kit);
                    frozen.add(id);
                    idxB++;
                }

                announceTeams(instance);
                startCountdown(instance);
            });
        });
        return true;
    }

    private void announceTeams(TeamDuelInstance instance) {
        Component teamA = namesOf(instance.getTeamA(), NamedTextColor.AQUA);
        Component teamB = namesOf(instance.getTeamB(), NamedTextColor.RED);
        Component header = Component.text("=== TEAM DUEL ===", NamedTextColor.GOLD, TextDecoration.BOLD);
        Component teamALine = Component.text("Team A: ", NamedTextColor.AQUA, TextDecoration.BOLD).append(teamA);
        Component teamBLine = Component.text("Team B: ", NamedTextColor.RED, TextDecoration.BOLD).append(teamB);
        for (UUID id : instance.getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.sendMessage(Component.empty());
            p.sendMessage(header);
            p.sendMessage(teamALine);
            p.sendMessage(teamBLine);
            p.sendMessage(Component.empty());
        }
    }

    private Component namesOf(Set<UUID> ids, NamedTextColor color) {
        Component out = Component.empty();
        boolean first = true;
        for (UUID id : ids) {
            Player p = Bukkit.getPlayer(id);
            String name = p != null ? p.getName() : id.toString().substring(0, 8);
            if (!first) out = out.append(Component.text(", ", NamedTextColor.GRAY));
            out = out.append(Component.text(name, color));
            first = false;
        }
        return out;
    }

    private Location spawnAround(Location base, int index, int teamSize) {
        if (teamSize <= 1) return base.clone();
        double angle = (2 * Math.PI * index) / teamSize;
        double r = 1.5;
        Location loc = base.clone();
        loc.add(Math.cos(angle) * r, 0, Math.sin(angle) * r);
        return loc;
    }

    private void failStart(TeamDuelInstance instance, String reason) {
        for (UUID id : instance.getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(Component.text(reason, NamedTextColor.RED));
            activeByPlayer.remove(id);
            frozen.remove(id);
        }
        activeById.remove(instance.getId());
        instance.setActive(false);
        if (instance.getInstanceWorld() != null) {
            plugin.getArenaManager().deleteInstanceWorld(instance.getInstanceWorldName());
        }
    }

    private void startCountdown(TeamDuelInstance instance) {
        new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (!instance.isActive()) {
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
                    for (UUID id : instance.getAllParticipants()) {
                        Player p = Bukkit.getPlayer(id);
                        if (p == null) continue;
                        p.showTitle(title);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                    }
                    count--;
                } else {
                    Title title = Title.title(
                            Component.text("FIGHT!", NamedTextColor.GREEN, TextDecoration.BOLD),
                            Component.empty(),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(1), Duration.ofMillis(500))
                    );
                    for (UUID id : instance.getAllParticipants()) {
                        Player p = Bukkit.getPlayer(id);
                        if (p == null) continue;
                        p.showTitle(title);
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
                        frozen.remove(id);
                    }
                    instance.setCountdownComplete(true);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    /**
     * Called from PlayerListener.onPlayerDeath when player is in a team duel.
     * Returns true if the death was consumed by team duel handling.
     */
    public boolean handleDeath(Player deceased) {
        TeamDuelInstance instance = activeByPlayer.get(deceased.getUniqueId());
        if (instance == null || !instance.isActive()) return false;

        instance.markDead(deceased.getUniqueId());

        TeamDuelInstance.Team winner = instance.getWinningTeam();
        if (winner != null) {
            endTeamDuel(instance, winner);
        } else {
            // Move dead player into spectator mode within the arena
            deadSpectators.add(deceased.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!deceased.isOnline()) return;
                deceased.getInventory().clear();
                deceased.setGameMode(GameMode.SPECTATOR);
                if (instance.getInstanceWorld() != null) {
                    deceased.teleport(instance.getInstanceWorld().getSpawnLocation());
                }
                deceased.sendMessage(Component.text("You died! Spectating until the match ends.", NamedTextColor.GRAY));
            });
        }
        return true;
    }

    private void endTeamDuel(TeamDuelInstance instance, TeamDuelInstance.Team winningTeam) {
        if (!instance.isActive()) return;
        instance.setActive(false);

        Set<UUID> winners = instance.getTeam(winningTeam);
        Set<UUID> losers = winningTeam == TeamDuelInstance.Team.A ? instance.getTeamB() : instance.getTeamA();

        for (UUID id : winners) plugin.getStatsManager().addWin(id);
        for (UUID id : losers) plugin.getStatsManager().addLoss(id);

        Title winTitle = Title.title(
                Component.text("VICTORY!", NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Your team won!", NamedTextColor.GREEN),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Title loseTitle = Title.title(
                Component.text("DEFEAT", NamedTextColor.RED, TextDecoration.BOLD),
                Component.text("Your team lost.", NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Component summary = Component.text("TEAM DUEL ", NamedTextColor.GOLD, TextDecoration.BOLD)
                .append(Component.text("| ", NamedTextColor.DARK_GRAY))
                .append(Component.text("Team " + winningTeam.name(), NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text(" wins!", NamedTextColor.GRAY));

        for (UUID id : winners) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.showTitle(winTitle);
                p.sendMessage(summary);
            }
        }
        for (UUID id : losers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.showTitle(loseTitle);
                p.sendMessage(summary);
            }
        }

        String instanceWorldName = instance.getInstanceWorldName();
        Set<UUID> all = instance.getAllParticipants();

        new BukkitRunnable() {
            @Override
            public void run() {
                Location lobby = plugin.getLobbyLocation();
                for (UUID id : all) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.isOnline()) {
                        p.getInventory().clear();
                        p.setHealth(p.getMaxHealth());
                        p.setFoodLevel(20);
                        p.setSaturation(20f);
                        p.setGameMode(GameMode.ADVENTURE);
                        p.teleport(lobby);
                    }
                    activeByPlayer.remove(id);
                    deadSpectators.remove(id);
                    frozen.remove(id);
                }
                activeById.remove(instance.getId());

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    plugin.getArenaManager().deleteInstanceWorld(instanceWorldName);
                }, 20L);
            }
        }.runTaskLater(plugin, 60L);
    }

    public void handleDisconnect(UUID playerId) {
        TeamDuelInstance instance = activeByPlayer.get(playerId);
        if (instance == null || !instance.isActive()) return;
        instance.markDead(playerId);
        TeamDuelInstance.Team winner = instance.getWinningTeam();
        if (winner != null) {
            endTeamDuel(instance, winner);
        }
        activeByPlayer.remove(playerId);
        deadSpectators.remove(playerId);
        frozen.remove(playerId);
    }

    public boolean isInTeamDuel(UUID playerId) {
        TeamDuelInstance i = activeByPlayer.get(playerId);
        return i != null && i.isActive();
    }

    public boolean isFrozen(UUID playerId) {
        return frozen.contains(playerId);
    }

    public boolean isDeadSpectator(UUID playerId) {
        return deadSpectators.contains(playerId);
    }

    public TeamDuelInstance getTeamDuelOf(UUID playerId) {
        return activeByPlayer.get(playerId);
    }

    public TeamDuelInstance getByWorld(String worldName) {
        for (TeamDuelInstance i : activeById.values()) {
            if (worldName.equals(i.getInstanceWorldName())) return i;
        }
        return null;
    }

    public boolean sameTeam(UUID a, UUID b) {
        TeamDuelInstance i = activeByPlayer.get(a);
        if (i == null) return false;
        if (!i.isParticipant(b)) return false;
        return i.sameTeam(a, b);
    }

    public void cleanupAll() {
        for (TeamDuelInstance instance : new HashSet<>(activeById.values())) {
            if (instance.isActive()) {
                instance.setActive(false);
                if (instance.getInstanceWorld() != null) {
                    plugin.getArenaManager().deleteInstanceWorld(instance.getInstanceWorldName());
                }
            }
        }
        activeById.clear();
        activeByPlayer.clear();
        deadSpectators.clear();
        frozen.clear();
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
