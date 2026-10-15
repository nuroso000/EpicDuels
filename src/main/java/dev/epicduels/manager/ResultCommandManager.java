package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Runs administrator-configured console commands after a completed duel.
 * Empty command lists are intentionally treated as disabled, which keeps the
 * feature backwards-compatible with existing configuration files.
 */
public class ResultCommandManager {

    private final EpicDuels plugin;

    public ResultCommandManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    public void execute(Collection<UUID> winners, Collection<UUID> losers,
                        String arena, String kit, int winnerScore, int loserScore) {
        runForPlayers("result-commands.winner", winners, arena, kit, winnerScore, loserScore);
        runForPlayers("result-commands.loser", losers, arena, kit, winnerScore, loserScore);
    }

    private void runForPlayers(String path, Collection<UUID> players,
                               String arena, String kit, int winnerScore, int loserScore) {
        List<String> templates = plugin.getConfig().getStringList(path);
        if (templates.isEmpty()) return;

        for (UUID playerId : players) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
            String playerName = player.getName();

            for (String template : templates) {
                if ((playerName == null || playerName.isBlank()) && template.contains("{player}")) {
                    plugin.getLogger().warning("Could not resolve the player name for result command: " + playerId);
                    continue;
                }
                String command = format(template, playerName, playerId, arena, kit, winnerScore, loserScore);
                if (command.startsWith("/")) command = command.substring(1);
                if (command.isBlank()) continue;

                try {
                    if (!Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)) {
                        plugin.getLogger().warning("Result command was not handled: " + command);
                    }
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Result command failed for " + playerId + ": " + ex.getMessage());
                }
            }
        }
    }

    private static String format(String template, String playerName, UUID playerId,
                                 String arena, String kit, int winnerScore, int loserScore) {
        return template
                .replace("{player}", playerName == null ? "" : playerName)
                .replace("{player_uuid}", playerId.toString())
                .replace("{arena}", arena)
                .replace("{kit}", kit)
                .replace("{winner_score}", Integer.toString(winnerScore))
                .replace("{loser_score}", Integer.toString(loserScore))
                .replace("{score}", winnerScore + ":" + loserScore);
    }
}
