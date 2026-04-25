package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {

    private final EpicDuels plugin;
    // kitName -> list of queued player UUIDs
    private final Map<String, List<UUID>> queues = new ConcurrentHashMap<>();
    // player UUID -> kit they're queued for
    private final Map<UUID, String> playerQueue = new ConcurrentHashMap<>();
    // player UUID -> timestamp when they joined queue
    private final Map<UUID, Long> queueJoinTime = new ConcurrentHashMap<>();
    private static final Random RANDOM = new Random();
    private BukkitTask actionBarTask;
    private BukkitTask matchmakingTask;

    public QueueManager(EpicDuels plugin) {
        this.plugin = plugin;
        startActionBarTask();
        startMatchmakingTask();
    }

    public boolean joinQueue(UUID playerId, String kitName) {
        if (playerQueue.containsKey(playerId)) return false;
        if (plugin.getDuelManager().isInDuel(playerId)) return false;

        queues.computeIfAbsent(kitName.toLowerCase(), k -> Collections.synchronizedList(new ArrayList<>())).add(playerId);
        playerQueue.put(playerId, kitName.toLowerCase());
        queueJoinTime.put(playerId, System.currentTimeMillis());
        return true;
    }

    public boolean leaveQueue(UUID playerId) {
        String kitName = playerQueue.remove(playerId);
        if (kitName == null) return false;

        List<UUID> queue = queues.get(kitName);
        if (queue != null) {
            queue.remove(playerId);
        }
        queueJoinTime.remove(playerId);
        return true;
    }

    public boolean isInQueue(UUID playerId) {
        return playerQueue.containsKey(playerId);
    }

    public String getQueuedKit(UUID playerId) {
        return playerQueue.get(playerId);
    }

    public int getQueueSize(String kitName) {
        List<UUID> queue = queues.get(kitName.toLowerCase());
        return queue != null ? queue.size() : 0;
    }

    public void removePlayer(UUID playerId) {
        leaveQueue(playerId);
    }

    private void startActionBarTask() {
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                Iterator<Map.Entry<UUID, String>> it = playerQueue.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<UUID, String> entry = it.next();
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player == null || !player.isOnline()) {
                        it.remove();
                        queueJoinTime.remove(entry.getKey());
                        String kit = entry.getValue();
                        List<UUID> q = queues.get(kit);
                        if (q != null) q.remove(entry.getKey());
                        continue;
                    }
                    String kitName = entry.getValue();
                    long joinTime = queueJoinTime.getOrDefault(entry.getKey(), System.currentTimeMillis());
                    long seconds = (System.currentTimeMillis() - joinTime) / 1000;

                    var kit = plugin.getKitManager().getKit(kitName);
                    String displayKit = kit != null ? kit.getName() : kitName;

                    player.sendActionBar(Component.text("Queue [" + displayKit + "]: searching for opponent... (" + seconds + "s)", NamedTextColor.YELLOW));
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void startMatchmakingTask() {
        matchmakingTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<String, List<UUID>> entry : queues.entrySet()) {
                    List<UUID> queue = entry.getValue();
                    if (queue.size() < 2) continue;

                    // Take first two players
                    UUID player1Id = queue.remove(0);
                    UUID player2Id = queue.remove(0);

                    String kitName = entry.getKey();
                    playerQueue.remove(player1Id);
                    playerQueue.remove(player2Id);
                    queueJoinTime.remove(player1Id);
                    queueJoinTime.remove(player2Id);

                    Player p1 = Bukkit.getPlayer(player1Id);
                    Player p2 = Bukkit.getPlayer(player2Id);

                    if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
                        // Put back the online one
                        if (p1 != null && p1.isOnline()) {
                            queue.add(0, player1Id);
                            playerQueue.put(player1Id, kitName);
                            queueJoinTime.put(player1Id, System.currentTimeMillis());
                        }
                        if (p2 != null && p2.isOnline()) {
                            queue.add(0, player2Id);
                            playerQueue.put(player2Id, kitName);
                            queueJoinTime.put(player2Id, System.currentTimeMillis());
                        }
                        continue;
                    }

                    // Pick a random ready arena
                    List<String> readyArenas = plugin.getArenaManager().getReadyArenaNames();
                    if (readyArenas.isEmpty()) {
                        p1.sendMessage(Component.text("No arenas available! Removed from queue.", NamedTextColor.RED));
                        p2.sendMessage(Component.text("No arenas available! Removed from queue.", NamedTextColor.RED));
                        p1.sendActionBar(Component.empty());
                        p2.sendActionBar(Component.empty());
                        continue;
                    }

                    String arenaName = readyArenas.get(RANDOM.nextInt(readyArenas.size()));

                    p1.sendActionBar(Component.empty());
                    p2.sendActionBar(Component.empty());
                    p1.sendMessage(Component.text("Match found! Starting duel against " + p2.getName() + "...", NamedTextColor.GREEN));
                    p2.sendMessage(Component.text("Match found! Starting duel against " + p1.getName() + "...", NamedTextColor.GREEN));

                    // Resolve kit name for display
                    var kit = plugin.getKitManager().getKit(kitName);
                    String resolvedKit = kit != null ? kit.getName() : kitName;

                    plugin.getDuelManager().startQueueDuel(p1, p2, arenaName, resolvedKit);
                }
            }
        }.runTaskTimer(plugin, 40L, 20L); // Check every second, start after 2s
    }

    public void cleanup() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        if (matchmakingTask != null) {
            matchmakingTask.cancel();
            matchmakingTask = null;
        }
        queues.clear();
        playerQueue.clear();
        queueJoinTime.clear();
    }
}
