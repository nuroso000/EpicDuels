package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.i18n.Messages;
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
import org.bukkit.scheduler.BukkitTask;

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
    private BukkitTask timeLimitTask;

    public TeamDuelManager(EpicDuels plugin) {
        this.plugin = plugin;
        startTimeLimitTask();
    }

    private void startTimeLimitTask() {
        timeLimitTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (TeamDuelInstance instance : activeById.values()) {
                    if (!instance.isActive() || !instance.isCountdownComplete()
                            || instance.getDeadlineMillis() <= 0) continue;

                    long remaining = instance.getDeadlineMillis() - System.currentTimeMillis();
                    if (remaining <= 0) {
                        endTeamDuelDraw(instance);
                    } else {
                        sendTimeActionBar(instance, remaining);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void sendTimeActionBar(TeamDuelInstance instance, long remainingMillis) {
        long seconds = (remainingMillis + 999) / 1000;
        NamedTextColor color = seconds <= 30 ? NamedTextColor.RED
                : seconds <= 60 ? NamedTextColor.YELLOW
                : NamedTextColor.GRAY;
        Component bar = Messages.get("duel.time-left",
                Messages.unparsed("time", formatTime(seconds))).color(color);
        for (UUID id : instance.getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendActionBar(bar);
        }
    }

    private static String formatTime(long totalSeconds) {
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    /**
     * Ends a team duel with no winner: no stats are recorded and everybody is
     * returned to the lobby.
     */
    private void endTeamDuelDraw(TeamDuelInstance instance) {
        if (!instance.isActive()) return;
        instance.setActive(false);

        Title drawTitle = Title.title(
                Messages.get("duel.draw-title"),
                Messages.get("duel.draw-subtitle"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Component summary = Messages.get("teamduel.summary-draw");

        for (UUID id : instance.getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                p.showTitle(drawTitle);
                p.sendMessage(summary);
            }
        }

        scheduleReturnToLobby(instance);
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
            // Safety: drop from queue/requests/spectating
            plugin.getQueueManager().removePlayer(id);
            plugin.getDuelManager().cancelRequest(id);
            plugin.getDuelManager().denyAllIncoming(id);
            plugin.getDuelManager().clearSpectatorState(id);
        }
        activeById.put(instance.getId(), instance);

        for (UUID id : instance.getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) Messages.send(p, "teamduel.preparing");
        }

        plugin.getArenaManager().createInstanceWorld(arena, instance.getInstanceWorldName()).thenAccept(world -> {
            if (world == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    failStart(instance, "teamduel.fail-create");
                });
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                instance.setInstanceWorld(world);
                instance.setActive(true);

                if (arena.getSpawn1() == null || arena.getSpawn2() == null) {
                    failStart(instance, "teamduel.fail-spawns");
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
        Component header = Messages.get("teamduel.header");
        Component teamALine = Messages.get("teamduel.team-a", Messages.component("names", teamA));
        Component teamBLine = Messages.get("teamduel.team-b", Messages.component("names", teamB));
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

    private void failStart(TeamDuelInstance instance, String reasonKey) {
        for (UUID id : instance.getAllParticipants()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) Messages.send(p, reasonKey);
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
            int count = Math.min(60, Math.max(0, plugin.getConfig().getInt("duel.countdown-seconds", 5)));

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
                            Messages.get("countdown.get-ready"),
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
                            Messages.get("countdown.fight"),
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

                    int timeLimit = plugin.getConfig().getInt("duel.time-limit-seconds", 300);
                    if (timeLimit > 0) {
                        instance.setDeadlineMillis(System.currentTimeMillis() + timeLimit * 1000L);
                    }

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
        return eliminate(deceased, Messages.get("teamduel.died"));
    }

    /**
     * Player gives up via /duel forfeit — they are out of the team duel and
     * spectate the rest of the match.
     */
    public void forfeit(Player player) {
        TeamDuelInstance instance = activeByPlayer.get(player.getUniqueId());
        if (instance == null || !instance.isActive()) return;
        if (!instance.isAlive(player.getUniqueId())) return;

        frozen.remove(player.getUniqueId());
        eliminate(player, Messages.get("teamduel.forfeited"));
    }

    /**
     * Removes a participant from the fight (death or forfeit). If their team is
     * wiped the duel ends, otherwise they spectate inside the arena.
     * Returns true if the player was an active participant.
     */
    private boolean eliminate(Player player, Component spectatorMessage) {
        TeamDuelInstance instance = activeByPlayer.get(player.getUniqueId());
        if (instance == null || !instance.isActive()) return false;

        instance.markDead(player.getUniqueId());

        TeamDuelInstance.Team winner = instance.getWinningTeam();
        if (winner != null) {
            endTeamDuel(instance, winner);
        } else {
            // Move the player into spectator mode within the arena
            deadSpectators.add(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!player.isOnline()) return;
                player.getInventory().clear();
                player.setGameMode(GameMode.SPECTATOR);
                if (instance.getInstanceWorld() != null) {
                    player.teleport(instance.getInstanceWorld().getSpawnLocation());
                }
                player.sendMessage(spectatorMessage);
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
                Messages.get("teamduel.victory-title"),
                Messages.get("teamduel.victory-subtitle"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Title loseTitle = Title.title(
                Messages.get("teamduel.defeat-title"),
                Messages.get("teamduel.defeat-subtitle"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofSeconds(1))
        );
        Component summary = Messages.get("teamduel.summary-win",
                Messages.unparsed("team", winningTeam.name()));

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

        scheduleReturnToLobby(instance);
    }

    /**
     * Waits 3 seconds, then returns all participants to the lobby and deletes
     * the instance world.
     */
    private void scheduleReturnToLobby(TeamDuelInstance instance) {
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

    /**
     * A living participant left the arena world (teleported away) — counts as
     * a death for their team.
     */
    public void handleForfeit(Player player) {
        TeamDuelInstance instance = activeByPlayer.get(player.getUniqueId());
        if (instance == null || !instance.isActive()) return;
        if (!instance.isAlive(player.getUniqueId())) return;

        instance.markDead(player.getUniqueId());
        frozen.remove(player.getUniqueId());
        Messages.send(player, "teamduel.left-arena");

        TeamDuelInstance.Team winner = instance.getWinningTeam();
        if (winner != null) {
            endTeamDuel(instance, winner);
        }
    }

    public void handleDisconnect(UUID playerId) {
        TeamDuelInstance instance = activeByPlayer.get(playerId);
        if (instance == null) return;
        // Also runs while the instance world is still being copied (instance
        // not active yet) — always drop the player so they aren't stuck busy.
        instance.markDead(playerId);
        if (instance.isActive()) {
            TeamDuelInstance.Team winner = instance.getWinningTeam();
            if (winner != null) {
                endTeamDuel(instance, winner);
            }
        }
        activeByPlayer.remove(playerId);
        deadSpectators.remove(playerId);
        frozen.remove(playerId);
    }

    public boolean isInTeamDuel(UUID playerId) {
        // Map presence (not isActive) so players count as busy while the
        // instance world is still being copied asynchronously and during the
        // short post-match phase before they are returned to the lobby.
        return activeByPlayer.containsKey(playerId);
    }

    /** True if any current team duel (active or starting) runs on the given arena. */
    public boolean isArenaInUse(String arenaName) {
        for (TeamDuelInstance i : activeById.values()) {
            if (i.getArenaName().equalsIgnoreCase(arenaName)) return true;
        }
        return false;
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
        if (timeLimitTask != null) {
            timeLimitTask.cancel();
            timeLimitTask = null;
        }
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
        // Use the player's personalized layout (Kit Editor) if one exists
        kit = plugin.getPlayerKitManager().getPersonalizedKit(player.getUniqueId(), kit);
        player.getInventory().setContents(kit.getContents());
        if (kit.getArmorContents() != null) {
            player.getInventory().setArmorContents(kit.getArmorContents());
        }
        if (kit.getOffHand() != null) {
            player.getInventory().setItemInOffHand(kit.getOffHand());
        }
    }
}
