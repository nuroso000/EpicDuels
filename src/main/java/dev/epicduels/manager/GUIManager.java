package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.Arena;
import dev.epicduels.model.Kit;
import dev.epicduels.model.PlayerStats;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GUIManager {

    public static final String MAIN_MENU_TITLE = "EpicDuels Menu";
    public static final String ARENA_SELECT_TITLE = "Select Map";
    public static final String KIT_SELECT_TITLE = "Select Kit";
    public static final String KIT_EDIT_TITLE = "Edit Kit: ";
    public static final String KIT_PREVIEW_TITLE = "Preview Kit: ";
    public static final String PLAYER_SELECT_TITLE = "Challenge Player";
    public static final String QUEUE_KIT_SELECT_TITLE = "Queue - Select Kit";

    private final EpicDuels plugin;

    // Track GUI state for challenge flow
    private final Map<UUID, UUID> challengeTarget = new HashMap<>();
    private final Map<UUID, String> challengeArena = new HashMap<>();
    private final Map<UUID, String> challengeKit = new HashMap<>();

    // Track random map animation state
    private final Set<UUID> animatingPlayers = new HashSet<>();

    public GUIManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    // ========== MAIN MENU (3-section layout in 54-slot chest) ==========

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(MAIN_MENU_TITLE, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        // Fill entire inventory with dark glass
        ItemStack darkPane = createPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, darkPane);
        }

        // Section dividers (columns 3 and 6 - slots with index % 9 == 3 or 6)
        ItemStack divider = createPane(Material.BLACK_STAINED_GLASS_PANE);
        for (int row = 0; row < 6; row++) {
            inv.setItem(row * 9 + 3, divider);
            inv.setItem(row * 9 + 5, divider);
        }

        // === LEFT SECTION: Challenge a Player (columns 0-2) ===
        ItemStack leftHeader = createItem(Material.DIAMOND_SWORD, "&a&lChallenge a Player", "&7Click to challenge someone!");
        inv.setItem(4 + 9 * 0, createItem(Material.NETHER_STAR, "&d&lEpicDuels", "&7Your one-stop duel menu"));

        // Challenge button in center of left section
        inv.setItem(9 + 1, leftHeader); // row 1, col 1
        inv.setItem(18 + 1, createItem(Material.GOLDEN_SWORD, "&eSelect Player", "&7Pick an opponent to fight"));
        inv.setItem(27 + 1, createItem(Material.IRON_SWORD, "&7Click above to begin", "&7a duel challenge!"));

        // === MIDDLE SECTION: Stats (columns 4) ===
        // Player head
        ItemStack playerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) playerHead.getItemMeta();
        headMeta.setOwningPlayer(player);
        headMeta.displayName(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        headMeta.lore(List.of(Component.text("Your Profile", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        playerHead.setItemMeta(headMeta);
        inv.setItem(9 + 4, playerHead); // row 1, col 4

        // Stats book
        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());
        ItemStack statsBook = new ItemStack(Material.BOOK);
        ItemMeta bookMeta = statsBook.getItemMeta();
        bookMeta.displayName(Component.text("Your Stats", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> statsLore = new ArrayList<>();
        statsLore.add(Component.empty());
        statsLore.add(Component.text("Wins: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getWins()), NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false));
        statsLore.add(Component.text("Losses: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getLosses()), NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false));
        statsLore.add(Component.text("Win Rate: ", NamedTextColor.GRAY).append(Component.text(String.format("%.1f%%", stats.getWinRate()), NamedTextColor.GOLD)).decoration(TextDecoration.ITALIC, false));
        statsLore.add(Component.text("Total Duels: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(stats.getTotalGames()), NamedTextColor.AQUA)).decoration(TextDecoration.ITALIC, false));
        statsLore.add(Component.empty());
        bookMeta.lore(statsLore);
        statsBook.setItemMeta(bookMeta);
        inv.setItem(27 + 4, statsBook); // row 3, col 4

        // === RIGHT SECTION: Queue / Matchmaking (columns 6-8) ===
        inv.setItem(9 + 7, createItem(Material.HOPPER, "&b&lQueue / Matchmaking", "&7Join a kit queue to", "&7auto-match with opponents"));

        // Queue kit buttons
        List<Kit> allKits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int[] queueSlots = {18 + 6, 18 + 7, 18 + 8, 27 + 6, 27 + 7, 27 + 8, 36 + 6, 36 + 7, 36 + 8};

        int idx = 0;
        for (Kit kit : allKits) {
            if (idx >= queueSlots.length - 2) break; // Leave room for special buttons
            int queueCount = plugin.getQueueManager().getQueueSize(kit.getName());
            String queueStatus = plugin.getQueueManager().isInQueue(player.getUniqueId())
                    && kit.getName().equalsIgnoreCase(plugin.getQueueManager().getQueuedKit(player.getUniqueId()))
                    ? "&aQueued!" : "&7" + queueCount + " in queue";
            inv.setItem(queueSlots[idx], createItem(kit.getDisplayIcon(), "&b" + kit.getName(), queueStatus, "&eClick to join/leave queue"));
            idx++;
        }

        // Random Kit button
        if (idx < queueSlots.length - 1) {
            inv.setItem(queueSlots[queueSlots.length - 2], createItem(Material.ENDER_PEARL, "&dRandom Kit", "&7Queue with a random kit"));
        }
        // No Kit button
        inv.setItem(queueSlots[queueSlots.length - 1], createItem(Material.BARRIER, "&cNo Kit", "&7Queue without a kit"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== PLAYER SELECT ==========

    public void openPlayerSelect(Player player) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.remove(player);

        int size = Math.min(54, Math.max(27, ((onlinePlayers.size() / 9) + 1) * 9 + 18));
        Inventory inv = Bukkit.createInventory(null, size, Component.text(PLAYER_SELECT_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));
        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);

        int slot = 10;
        for (Player target : onlinePlayers) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName(), NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to challenge", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            head.setItemMeta(meta);
            inv.setItem(slot, head);
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== KIT SELECT (for challenge flow) ==========

    public void openKitSelect(Player player, UUID targetPlayer) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);

        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int size = Math.max(27, ((kits.size() / 7) + 1) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text(KIT_SELECT_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int slot = 10;
        for (Kit kit : kits) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            inv.setItem(slot, createItem(kit.getDisplayIcon(), "&b" + kit.getName(), "&7Click to select this kit"));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== MAP/ARENA SELECT (after kit selection) ==========

    public void openArenaSelect(Player player, UUID targetPlayer, String kitName) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);
        challengeKit.put(player.getUniqueId(), kitName);

        List<Arena> readyArenas = plugin.getArenaManager().getReadyArenas();
        int size = Math.max(27, ((readyArenas.size() / 7) + 2) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text(ARENA_SELECT_TITLE, NamedTextColor.GREEN, TextDecoration.BOLD));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        int slot = 10;
        for (Arena arena : readyArenas) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            inv.setItem(slot, createItem(arena.getDisplayIcon(), "&a" + arena.getName(), "&7Click to select this map"));
            slot++;
        }

        // Random Map button in last row center
        inv.setItem(size - 5, createItem(Material.COMPASS, "&e&lRandom Map", "&7Randomly picks a map", "&7with a fun animation!"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== RANDOM MAP ANIMATION ==========

    public void startRandomMapAnimation(Player player) {
        List<Arena> readyArenas = plugin.getArenaManager().getReadyArenas();
        if (readyArenas.isEmpty()) {
            player.sendMessage(Component.text("No arenas available!", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        animatingPlayers.add(player.getUniqueId());

        // Choose the final arena
        Arena chosenArena = readyArenas.get(new Random().nextInt(readyArenas.size()));

        Inventory inv = player.getOpenInventory().getTopInventory();
        int animSlot = inv.getSize() / 2; // Center slot

        // Clear center area for animation
        inv.setItem(animSlot, new ItemStack(Material.AIR));

        new BukkitRunnable() {
            int ticks = 0;
            int totalTicks = 50; // ~2.5 seconds at varying speed
            int currentDelay = 2; // Start fast (100ms = 2 ticks)
            int ticksSinceLastChange = 0;
            int arenaIndex = 0;

            @Override
            public void run() {
                if (!player.isOnline() || !animatingPlayers.contains(player.getUniqueId())) {
                    animatingPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                ticksSinceLastChange++;

                if (ticksSinceLastChange >= currentDelay) {
                    ticksSinceLastChange = 0;
                    Arena displayArena = readyArenas.get(arenaIndex % readyArenas.size());
                    arenaIndex++;

                    inv.setItem(animSlot, createItem(displayArena.getDisplayIcon(), "&e" + displayArena.getName(), "&7Selecting..."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.5f);

                    ticks++;

                    // Gradually slow down
                    if (ticks > 15) currentDelay = 3;
                    if (ticks > 22) currentDelay = 4;
                    if (ticks > 27) currentDelay = 6;
                    if (ticks > 30) currentDelay = 8;
                    if (ticks > 33) currentDelay = 12;

                    if (ticks > 35) {
                        // Final selection
                        inv.setItem(animSlot, createItem(chosenArena.getDisplayIcon(), "&a&l" + chosenArena.getName(), "&aSelected!"));
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        animatingPlayers.remove(player.getUniqueId());

                        // After a short delay, proceed with the challenge
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                player.closeInventory();
                                finishChallengeWithArena(player, chosenArena.getName());
                            }
                        }, 30L);

                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ========== KIT EDIT / PREVIEW ==========

    public void openKitEdit(Player player, Kit kit) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(KIT_EDIT_TITLE + kit.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));

        ItemStack[] contents = kit.getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) {
                inv.setItem(i, contents[i].clone());
            }
        }

        if (kit.getArmorContents() != null) {
            ItemStack[] armor = kit.getArmorContents();
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null) {
                    inv.setItem(36 + i, armor[i].clone());
                }
            }
        }

        if (kit.getOffHand() != null) {
            inv.setItem(40, kit.getOffHand().clone());
        }

        inv.setItem(53, createItem(Material.EMERALD, "&aSave Kit", "&7Click to save changes"));

        player.openInventory(inv);
    }

    public void openKitPreview(Player player, Kit kit) {
        Inventory inv = Bukkit.createInventory(null, 54, Component.text(KIT_PREVIEW_TITLE + kit.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        ItemStack[] contents = kit.getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) {
                inv.setItem(i + 9, contents[i].clone());
            }
        }

        if (kit.getArmorContents() != null) {
            ItemStack[] armor = kit.getArmorContents();
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null) {
                    inv.setItem(45 + i, armor[i].clone());
                }
            }
        }

        if (kit.getOffHand() != null) {
            inv.setItem(49, kit.getOffHand().clone());
        }

        player.openInventory(inv);
    }

    public void openKitList(Player player) {
        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int size = Math.max(27, ((kits.size() / 7) + 1) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Kits", NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int slot = 10;
        for (Kit kit : kits) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            inv.setItem(slot, createItem(kit.getDisplayIcon(), "&b" + kit.getName(), "&7Click to preview"));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openArenaList(Player player) {
        Collection<Arena> arenas = plugin.getArenaManager().getAllArenas();
        int size = Math.max(27, ((arenas.size() / 7) + 1) * 9 + 18);
        size = Math.min(54, size);
        Inventory inv = Bukkit.createInventory(null, size, Component.text("Arenas", NamedTextColor.GREEN, TextDecoration.BOLD));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        int slot = 10;
        for (Arena arena : arenas) {
            if (slot >= size - 9) break;
            if (slot % 9 == 0) slot++;
            if (slot % 9 == 8) slot += 2;

            String status = arena.isReady() ? "&aReady" : "&cIncomplete";
            inv.setItem(slot, createItem(arena.getDisplayIcon(), "&a" + arena.getName(), status));
            slot++;
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== Challenge flow helpers ==========

    public void finishChallengeWithArena(Player player, String arenaName) {
        UUID targetUUID = challengeTarget.get(player.getUniqueId());
        String kitName = challengeKit.get(player.getUniqueId());

        clearChallengeData(player.getUniqueId());

        if (targetUUID == null || kitName == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player is no longer online.", NamedTextColor.RED));
            return;
        }

        boolean sent = plugin.getDuelManager().sendRequest(player.getUniqueId(), targetUUID, arenaName, kitName);
        if (!sent) {
            player.sendMessage(Component.text("Could not send duel request. You may already have a pending request.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Duel request sent to " + target.getName() + "!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Arena: " + arenaName + " | Kit: " + kitName, NamedTextColor.GRAY));

        target.sendMessage(Component.empty());
        target.sendMessage(Component.text("=========================", NamedTextColor.GOLD));
        target.sendMessage(Component.text(player.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" challenged you to a duel!", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("Arena: ", NamedTextColor.GRAY)
                .append(Component.text(arenaName, NamedTextColor.WHITE))
                .append(Component.text(" | Kit: ", NamedTextColor.GRAY))
                .append(Component.text(kitName, NamedTextColor.WHITE)));
        target.sendMessage(Component.text("[ACCEPT]", NamedTextColor.GREEN)
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/duel accept " + player.getName()))
                .append(Component.text("  "))
                .append(Component.text("[DENY]", NamedTextColor.RED)
                        .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/duel deny " + player.getName()))));
        target.sendMessage(Component.text("Expires in 30 seconds!", NamedTextColor.GRAY));
        target.sendMessage(Component.text("=========================", NamedTextColor.GOLD));
        target.sendMessage(Component.empty());

        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    public UUID getChallengeTarget(UUID player) {
        return challengeTarget.get(player);
    }

    public String getChallengeArena(UUID player) {
        return challengeArena.get(player);
    }

    public String getChallengeKit(UUID player) {
        return challengeKit.get(player);
    }

    public void setChallengeKit(UUID player, String kit) {
        challengeKit.put(player, kit);
    }

    public void clearChallengeData(UUID player) {
        challengeTarget.remove(player);
        challengeArena.remove(player);
        challengeKit.remove(player);
        animatingPlayers.remove(player);
    }

    public boolean isAnimating(UUID player) {
        return animatingPlayers.contains(player);
    }

    // ========== Utility methods ==========

    private void fillBorder(Inventory inv, Material pane) {
        ItemStack border = createPane(pane);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, border.clone());
        }
        for (int i = size - 9; i < size; i++) {
            inv.setItem(i, border.clone());
        }
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, border.clone());
            inv.setItem(i + 8, border.clone());
        }
    }

    private ItemStack createPane(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createItem(Material material, String name, String... loreLines) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(colorize(name).decoration(TextDecoration.ITALIC, false));
        if (loreLines.length > 0) {
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                if (line != null && !line.isEmpty()) {
                    lore.add(colorize(line).decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Component colorize(String text) {
        NamedTextColor color = NamedTextColor.WHITE;
        boolean bold = false;
        StringBuilder result = new StringBuilder();
        Component component = Component.empty();

        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '&' && i + 1 < text.length()) {
                if (!result.isEmpty()) {
                    Component part = Component.text(result.toString(), color);
                    if (bold) part = part.decorate(TextDecoration.BOLD);
                    component = component.append(part);
                    result = new StringBuilder();
                }
                char code = text.charAt(i + 1);
                if (code == 'l') {
                    bold = true;
                } else {
                    bold = false;
                    color = switch (code) {
                        case '0' -> NamedTextColor.BLACK;
                        case '1' -> NamedTextColor.DARK_BLUE;
                        case '2' -> NamedTextColor.DARK_GREEN;
                        case '3' -> NamedTextColor.DARK_AQUA;
                        case '4' -> NamedTextColor.DARK_RED;
                        case '5' -> NamedTextColor.DARK_PURPLE;
                        case '6' -> NamedTextColor.GOLD;
                        case '7' -> NamedTextColor.GRAY;
                        case '8' -> NamedTextColor.DARK_GRAY;
                        case '9' -> NamedTextColor.BLUE;
                        case 'a' -> NamedTextColor.GREEN;
                        case 'b' -> NamedTextColor.AQUA;
                        case 'c' -> NamedTextColor.RED;
                        case 'd' -> NamedTextColor.LIGHT_PURPLE;
                        case 'e' -> NamedTextColor.YELLOW;
                        case 'f' -> NamedTextColor.WHITE;
                        default -> color;
                    };
                }
                i++;
            } else {
                result.append(text.charAt(i));
            }
        }
        if (!result.isEmpty()) {
            Component part = Component.text(result.toString(), color);
            if (bold) part = part.decorate(TextDecoration.BOLD);
            component = component.append(part);
        }
        return component;
    }
}
