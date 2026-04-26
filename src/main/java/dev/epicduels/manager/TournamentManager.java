package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import dev.epicduels.model.DuelInstance;
import dev.epicduels.model.Kit;
import dev.epicduels.model.Party;
import dev.epicduels.model.Tournament;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TournamentManager {

    private final EpicDuels plugin;
    private final Map<UUID, Tournament> tournaments = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToTournament = new ConcurrentHashMap<>();
    // Eliminated player -> currently watching this duel instance id (or null waiting in lobby)
    private final Map<UUID, UUID> eliminatedSpectatingDuelId = new ConcurrentHashMap<>();

    public TournamentManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    public boolean startTournament(Party party, Kit kit) {
        if (party == null) return false;
        List<UUID> participants = new ArrayList<>(party.getMembers());
        if (participants.size() < 2) return false;

        Tournament t = new Tournament(party.getId(), participants, kit.getName());
        tournaments.put(t.getId(), t);
        for (UUID id : participants) {
            playerToTournament.put(id, t.getId());
            // Safety: clear other states
            plugin.getQueueManager().removePlayer(id);
            plugin.getDuelManager().cancelRequest(id);
            plugin.getDuelManager().denyRequest(id);
        }

        party.messageAll(Component.text("=== TOURNAMENT STARTED ===", NamedTextColor.GOLD, TextDecoration.BOLD));
        party.messageAll(Component.text(participants.size() + " players, kit: " + kit.getName(), NamedTextColor.GRAY));

        buildAndStartRound(t, new ArrayList<>(participants));
        return true;
    }

    private void buildAndStartRound(Tournament t, List<UUID> players) {
        t.incrementRound();
        Collections.shuffle(players);

        Party party = plugin.getPartyManager().getParty(t.getPartyId());
        List<UUID> nextRoundWinners = new ArrayList<>();
        t.setCurrentRoundWinners(nextRoundWinners);
        t.getActiveMatches().clear();
        t.getPendingMatches().clear();

        // Pair players, last odd one gets a bye
        int i = 0;
        while (i + 1 < players.size()) {
            t.getPendingMatches().add(new Tournament.Match(players.get(i), players.get(i + 1)));
            i += 2;
        }
        if (i < players.size()) {
            UUID byePlayer = players.get(i);
            nextRoundWinners.add(byePlayer);
            Player p = Bukkit.getPlayer(byePlayer);
            if (p != null) {
                p.sendMessage(Component.text("You got a bye for round " + t.getRoundNumber() + ".", NamedTextColor.YELLOW));
            }
        }

        if (party != null) {
            party.messageAll(Component.text("Round " + t.getRoundNumber() + " — " + t.getPendingMatches().size() + " match(es)", NamedTextColor.AQUA));
        }

        launchPendingMatches(t);
    }

    private void launchPendingMatches(Tournament t) {
        Kit kit = plugin.getKitManager().getKit(t.getKitName());
        if (kit == null) {
            failTournament(t, "Kit no longer exists.");
            return;
        }
        List<Arena> readyArenas = new ArrayList<>(plugin.getArenaManager().getReadyArenas());
        if (readyArenas.isEmpty()) {
            failTournament(t, "No ready arenas available!");
            return;
        }
        Collections.shuffle(readyArenas);

        Iterator<Tournament.Match> it = t.getPendingMatches().iterator();
        int arenaIdx = 0;
        while (it.hasNext()) {
            Tournament.Match match = it.next();
            Player p1 = Bukkit.getPlayer(match.p1);
            Player p2 = Bukkit.getPlayer(match.p2);
            if (p1 == null || p2 == null) {
                // Auto-advance the present one (or none) — both offline = drop
                it.remove();
                if (p1 != null && p2 == null) {
                    t.getCurrentRoundWinners().add(match.p1);
                    p1.sendMessage(Component.text("Opponent offline — auto-advance.", NamedTextColor.YELLOW));
                } else if (p2 != null && p1 == null) {
                    t.getCurrentRoundWinners().add(match.p2);
                    p2.sendMessage(Component.text("Opponent offline — auto-advance.", NamedTextColor.YELLOW));
                }
                continue;
            }
            Arena arena = readyArenas.get(arenaIdx % readyArenas.size());
            arenaIdx++;

            DuelInstance duel = plugin.getDuelManager().startQueueDuelWithCallback(p1, p2, arena.getName(), kit.getName(),
                    (winner, loser) -> onMatchEnd(t, match, winner, loser));
            if (duel == null) {
                it.remove();
                continue;
            }
            match.started = true;
            match.duelInstanceId = duel.getId();
            t.getActiveMatches().add(match);
            it.remove();

            p1.sendMessage(Component.text("Round " + t.getRoundNumber() + ": you vs " + p2.getName(), NamedTextColor.AQUA));
            p2.sendMessage(Component.text("Round " + t.getRoundNumber() + ": you vs " + p1.getName(), NamedTextColor.AQUA));
        }

        // Re-route eliminated spectators to new active matches
        rerouteEliminatedSpectators(t, null);

        // If all matches finished synchronously (e.g. all dropped), advance immediately
        checkRoundCompletion(t);
    }

    private void onMatchEnd(Tournament t, Tournament.Match match, UUID winnerId, UUID loserId) {
        if (t.isFinished()) return;
        match.finished = true;
        match.winner = winnerId;
        t.getActiveMatches().remove(match);
        t.getCurrentRoundWinners().add(winnerId);

        // Loser becomes eliminated
        if (loserId != null) {
            t.getEliminated().add(loserId);
            // Schedule reroute slightly after duel cleanup so DuelManager has cleared spectators
            UUID loserCopy = loserId;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player loserP = Bukkit.getPlayer(loserCopy);
                if (loserP != null && loserP.isOnline() && playerToTournament.containsKey(loserCopy)) {
                    routeEliminatedToSpectate(t, loserCopy);
                }
            }, 80L); // 4s after end (DuelManager teleports lobby ~3s, give a buffer)
        }

        // Re-route any spectators that were watching this just-ended match
        rerouteEliminatedSpectators(t, match.duelInstanceId);

        Player wp = Bukkit.getPlayer(winnerId);
        if (wp != null) {
            wp.sendMessage(Component.text("You advanced to the next round!", NamedTextColor.GREEN));
        }

        checkRoundCompletion(t);
    }

    private void checkRoundCompletion(Tournament t) {
        if (!t.getActiveMatches().isEmpty()) return;
        if (!t.getPendingMatches().isEmpty()) return;

        List<UUID> winners = new ArrayList<>(t.getCurrentRoundWinners());
        if (winners.size() <= 1) {
            // Tournament complete
            UUID champion = winners.isEmpty() ? null : winners.get(0);
            finishTournament(t, champion);
            return;
        }
        // Next round
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (t.isFinished()) return;
            buildAndStartRound(t, winners);
        }, 100L); // 5s pause between rounds
    }

    private void routeEliminatedToSpectate(Tournament t, UUID eliminatedId) {
        Player p = Bukkit.getPlayer(eliminatedId);
        if (p == null || !p.isOnline()) return;
        if (t.isFinished()) return;

        // Pick a random active match's duel instance
        List<Tournament.Match> active = new ArrayList<>(t.getActiveMatches());
        if (active.isEmpty()) {
            // No live match — leave them in lobby
            eliminatedSpectatingDuelId.remove(eliminatedId);
            return;
        }
        Tournament.Match target = active.get(new Random().nextInt(active.size()));
        DuelInstance targetDuel = plugin.getDuelManager().getDuelById(target.duelInstanceId);
        if (targetDuel == null || !targetDuel.isActive()) {
            eliminatedSpectatingDuelId.remove(eliminatedId);
            return;
        }
        eliminatedSpectatingDuelId.put(eliminatedId, targetDuel.getId());
        plugin.getDuelManager().addSpectator(p, targetDuel);
    }

    private void rerouteEliminatedSpectators(Tournament t, UUID endedDuelId) {
        // Move eliminated players who were watching the just-ended match (or any non-existent match)
        for (Map.Entry<UUID, UUID> entry : new HashMap<>(eliminatedSpectatingDuelId).entrySet()) {
            UUID specId = entry.getKey();
            if (!playerToTournament.getOrDefault(specId, new UUID(0, 0)).equals(t.getId())) continue;
            UUID watchingId = entry.getValue();
            if (endedDuelId != null && !watchingId.equals(endedDuelId)) continue;
            Bukkit.getScheduler().runTaskLater(plugin, () -> routeEliminatedToSpectate(t, specId), 60L);
        }
    }

    private void finishTournament(Tournament t, UUID championId) {
        t.setFinished(true);
        t.setChampion(championId);

        Party party = plugin.getPartyManager().getParty(t.getPartyId());
        String championName = "Nobody";
        if (championId != null) {
            Player cp = Bukkit.getPlayer(championId);
            championName = cp != null ? cp.getName() : Bukkit.getOfflinePlayer(championId).getName();
            if (championName == null) championName = championId.toString().substring(0, 8);
        }

        Component announce1 = Component.text("=== TOURNAMENT WINNER ===", NamedTextColor.GOLD, TextDecoration.BOLD);
        Component announce2 = Component.text(championName + " wins the tournament!", NamedTextColor.YELLOW, TextDecoration.BOLD);
        Title title = Title.title(
                Component.text(championName, NamedTextColor.GOLD, TextDecoration.BOLD),
                Component.text("Tournament Champion!", NamedTextColor.YELLOW),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(4), Duration.ofSeconds(1))
        );

        if (party != null) {
            for (UUID memberId : party.getMembers()) {
                Player p = Bukkit.getPlayer(memberId);
                if (p == null || !p.isOnline()) continue;
                p.sendMessage(Component.empty());
                p.sendMessage(announce1);
                p.sendMessage(announce2);
                p.sendMessage(Component.empty());
                p.showTitle(title);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }

        // Send eliminated spectators back to lobby
        cleanupTournamentState(t);
    }

    private void failTournament(Tournament t, String reason) {
        t.setFinished(true);
        Party party = plugin.getPartyManager().getParty(t.getPartyId());
        if (party != null) {
            party.messageAll(Component.text("Tournament cancelled: " + reason, NamedTextColor.RED));
        }
        cleanupTournamentState(t);
    }

    private void cleanupTournamentState(Tournament t) {
        for (UUID id : t.getInitialParticipants()) {
            playerToTournament.remove(id);
            UUID watchingId = eliminatedSpectatingDuelId.remove(id);
            if (watchingId != null) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline() && plugin.getDuelManager().isSpectating(id)) {
                    plugin.getDuelManager().removeSpectator(id);
                } else if (p != null && p.isOnline()) {
                    p.setGameMode(GameMode.ADVENTURE);
                    p.teleport(plugin.getLobbyLocation());
                }
            }
        }
        tournaments.remove(t.getId());
    }

    public void handleDisconnect(UUID playerId) {
        UUID tid = playerToTournament.get(playerId);
        if (tid == null) return;
        Tournament t = tournaments.get(tid);
        if (t == null) return;
        // Find any active match player belongs to — DuelManager.handleDisconnect will end that duel,
        // which fires our onMatchEnd callback. So we just need to drop spectator state and tournament link.
        // If player was eliminated and just spectating, remove from tracking.
        eliminatedSpectatingDuelId.remove(playerId);
        // Don't immediately remove from tournament — onMatchEnd will. But if they had a bye and weren't
        // in any match, drop them from currentRoundWinners.
        t.getCurrentRoundWinners().remove(playerId);
        playerToTournament.remove(playerId);
    }

    public boolean isInTournament(UUID playerId) {
        return playerToTournament.containsKey(playerId);
    }

    public void cleanupAll() {
        for (Tournament t : new ArrayList<>(tournaments.values())) {
            t.setFinished(true);
            cleanupTournamentState(t);
        }
        tournaments.clear();
        playerToTournament.clear();
        eliminatedSpectatingDuelId.clear();
    }
}
