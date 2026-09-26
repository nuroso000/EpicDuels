package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.gui.PresentationText;
import dev.epicduels.model.*;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

/** Main-thread sidebar rendering. Unique invisible entries preserve duplicate and blank lines. */
public final class ScoreboardManager implements Listener {
    private final EpicDuels plugin;
    private YamlConfiguration config;
    private BukkitTask task;
    private final Map<UUID, Board> boards = new HashMap<>();
    private final Set<String> warnings = new HashSet<>();
    private static final class Board {
        final Scoreboard previous;
        final Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        final Objective objective = scoreboard.registerNewObjective("epicduels", Criteria.DUMMY, Component.empty());
        List<Component> rendered = List.of();
        Board(Scoreboard previous) { this.previous = previous; objective.setDisplaySlot(DisplaySlot.SIDEBAR); }
    }
    public ScoreboardManager(EpicDuels plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        reload();
    }
    public void reload() {
        if (task != null) task.cancel();
        config = PresentationConfig.load(plugin, "scoreboard.yml", config);
        warnings.clear();
        int interval = config.getInt("update-interval-ticks", 20);
        if (interval < 10 || interval > 1200) { warn("interval", "update-interval-ticks must be 10..1200; using 20"); interval = 20; }
        refresh();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, interval, interval);
    }
    private void warn(String key, String message) {
        if (warnings.add(key)) plugin.getLogger().warning("scoreboard.yml: " + message);
    }
    public void refresh() {
        for (Player player : Bukkit.getOnlinePlayers()) update(player);
    }
    private String name(UUID id) { Player player = Bukkit.getPlayer(id); return player == null ? "Offline" : player.getName(); }
    private String time(long deadline) {
        if (deadline == 0) return "--:--";
        long seconds = Math.max(0, (deadline - System.currentTimeMillis() + 999) / 1000);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
    private void update(Player player) {
        UUID id = player.getUniqueId();
        String view = "lobby";
        Map<String, String> values = new HashMap<>();
        for (String key : List.of("opponent", "kit", "arena", "round", "score", "opponent_score", "time", "best_of")) values.put(key, "-");
        values.put("player", player.getName());
        DuelInstance duel = plugin.getDuelManager().getDuel(id);
        if (duel == null) { duel = plugin.getDuelManager().getSpectatedDuel(id); if (duel != null) view = "spectator"; }
        if (duel != null && !duel.isActive()) { duel = null; view = "lobby"; }
        if (duel != null) {
            if (view.equals("lobby")) view = "duel";
            UUID perspective = duel.isParticipant(id) ? id : duel.getPlayer1();
            values.put("opponent", name(duel.getOpponent(perspective)));
            values.put("kit", Kit.displayName(duel.getKitName())); values.put("arena", duel.getArenaName());
            values.put("round", String.valueOf(duel.getCurrentRound())); values.put("best_of", String.valueOf(duel.getBestOf()));
            values.put("score", String.valueOf(duel.getWins(perspective)));
            values.put("opponent_score", String.valueOf(duel.getWins(duel.getOpponent(perspective))));
            values.put("time", time(duel.getDeadlineMillis()));
        } else {
            TeamDuelInstance team = plugin.getTeamDuelManager().getTeamDuelOf(id);
            if (team != null && team.isActive()) {
                view = "duel"; values.put("kit", Kit.displayName(team.getKitName())); values.put("arena", team.getArenaName());
                values.put("opponent", "Opposing team"); values.put("time", time(team.getDeadlineMillis()));
            } else if (plugin.getQueueManager().isInQueue(id)) {
                view = "queue"; values.put("kit", plugin.getQueueManager().getQueuedKit(id));
            }
        }
        String path = "views." + view;
        if (!config.getBoolean("enabled", true) || !config.getBoolean(path + ".enabled", true) || !config.isList(path + ".lines")) { clear(player); return; }
        List<String> lines = config.getStringList(path + ".lines");
        if (lines.size() > 15) { warn(path, path + " has more than 15 lines; truncating"); lines = lines.subList(0, 15); }
        List<Component> rendered = new ArrayList<>();
        try {
            rendered.add(PresentationText.render(config.getString(path + ".title", "<gold>EpicDuels</gold>"), values));
            for (String line : lines) rendered.add(PresentationText.render(line, values));
        } catch (RuntimeException error) { warn(path, path + " contains invalid text; clearing sidebar"); clear(player); return; }
        Board board = boards.computeIfAbsent(id, key -> new Board(player.getScoreboard()));
        if (!rendered.equals(board.rendered)) {
            board.objective.displayName(rendered.getFirst());
            for (int index = 0; index < 15; index++) {
                String entry = "§" + Integer.toHexString(index) + "§r";
                Team row = board.scoreboard.getTeam("line" + index);
                if (index < lines.size()) {
                    if (row == null) { row = board.scoreboard.registerNewTeam("line" + index); row.addEntry(entry); }
                    row.prefix(rendered.get(index + 1));
                    board.objective.getScore(entry).setScore(lines.size() - index);
                } else { board.scoreboard.resetScores(entry); if (row != null) row.unregister(); }
            }
            board.rendered = List.copyOf(rendered);
        }
        if (player.getScoreboard() != board.scoreboard) player.setScoreboard(board.scoreboard);
    }
    public void clear(Player player) {
        Board board = boards.remove(player.getUniqueId());
        if (board != null && player.getScoreboard() == board.scoreboard) player.setScoreboard(board.previous);
    }
    @EventHandler public void onQuit(PlayerQuitEvent event) { clear(event.getPlayer()); }
    public void shutdown() { if (task != null) task.cancel(); for (Player player : Bukkit.getOnlinePlayers()) clear(player); boards.clear(); }
}
