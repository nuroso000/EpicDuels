package dev.epicduels.hook;

import dev.epicduels.EpicDuels;
import dev.epicduels.manager.StatsManager;
import dev.epicduels.model.PlayerStats;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/**
 * PlaceholderAPI expansion. Registered from onEnable only when PlaceholderAPI
 * is installed (this class must never be classloaded without it).
 *
 * Placeholders:
 *   %epicduels_wins%     — total wins
 *   %epicduels_losses%   — total losses
 *   %epicduels_winrate%  — win rate percentage, one decimal (e.g. "62.5")
 *   %epicduels_score%    — leaderboard score (wins² / (wins + losses))
 *   %epicduels_in_duel%  — "true" if in a duel, team duel or tournament
 */
public class EpicDuelsExpansion extends PlaceholderExpansion {

    private final EpicDuels plugin;

    public EpicDuelsExpansion(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "epicduels";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        // Survive PlaceholderAPI reloads — the plugin re-registers on enable
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) return null;

        return switch (params.toLowerCase()) {
            case "wins" -> String.valueOf(stats(player).getWins());
            case "losses" -> String.valueOf(stats(player).getLosses());
            case "winrate" -> String.format("%.1f", stats(player).getWinRate());
            case "score" -> String.valueOf(StatsManager.calculateScore(stats(player)));
            case "in_duel" -> String.valueOf(plugin.getDuelManager().isBusy(player.getUniqueId()));
            default -> null;
        };
    }

    private PlayerStats stats(OfflinePlayer player) {
        return plugin.getStatsManager().getStats(player.getUniqueId());
    }
}
