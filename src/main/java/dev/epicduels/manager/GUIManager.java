package dev.epicduels.manager;

import dev.epicduels.EpicDuels;
import dev.epicduels.i18n.Messages;
import dev.epicduels.gui.MenuHolder;
import dev.epicduels.gui.MenuKeys;
import dev.epicduels.gui.MenuType;
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

    // GUI title constants
    public static final String MAIN_MENU_TITLE = "EpicDuels";
    public static final String DUELS_MENU_TITLE = "Duels - Select Player";
    public static final String STATS_MENU_TITLE = "Your Stats";
    public static final String MATCHMAKING_TITLE = "Matchmaking";
    public static final String KIT_SELECT_TITLE = "Select Kit";
    public static final String ROUNDS_SELECT_TITLE = "Select Rounds";
    public static final String ARENA_SELECT_TITLE = "Select Map";
    public static final String KIT_EDIT_TITLE = "Edit Kit: ";
    public static final String KIT_PREVIEW_TITLE = "Preview Kit: ";
    public static final String KIT_LIST_TITLE = "Kits";
    public static final String KIT_EDITOR_LIST_TITLE = "Kit Editor";
    public static final String KIT_CUSTOMIZE_TITLE = "Customize Kit: ";

    // Customize GUI control slots (bottom row, after the 41 kit slots)
    public static final int CUSTOMIZE_INFO_SLOT = 50;
    public static final int CUSTOMIZE_RESET_SLOT = 51;
    public static final int CUSTOMIZE_SAVE_SLOT = 53;
    public static final String ARENA_LIST_TITLE = "Arenas";
    public static final String SPECTATE_TITLE = "Spectate - Live Duels";
    public static final String PARTY_MODE_TITLE = "Party - Choose Mode";
    public static final String PARTY_TEAM_SIZE_TITLE = "Party - Team Size";
    public static final String PARTY_KIT_TITLE = "Party - Select Kit";
    public static final String PARTY_CONFIRM_TITLE = "Party - Confirm";

    // Paginated menu: item slots (rows 1-4, columns 1-7 in a 54-slot chest)
    public static final int[] ITEM_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };
    public static final int ITEMS_PER_PAGE = ITEM_SLOTS.length; // 28
    public static final int PREV_SLOT = 45;
    public static final int BACK_SLOT = 49;
    public static final int NEXT_SLOT = 53;

    private final EpicDuels plugin;

    // Challenge flow state
    private final Map<UUID, UUID> challengeTarget = new HashMap<>();
    private final Map<UUID, String> challengeKit = new HashMap<>();
    private final Map<UUID, Integer> challengeRounds = new HashMap<>();

    // Random map animation state
    private final Set<UUID> animatingPlayers = new HashSet<>();

    // Party flow state (per owner)
    private final Map<UUID, dev.epicduels.model.PartyMode> partyFlowMode = new HashMap<>();
    private final Map<UUID, dev.epicduels.model.TeamSize> partyFlowTeamSize = new HashMap<>();
    private final Map<UUID, String> partyFlowKit = new HashMap<>();

    public GUIManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    /** Creates a plugin menu inventory identified by a {@link MenuHolder}. */
    private Inventory createMenu(MenuType type, int size, Component title) {
        return createMenu(type, null, 0, size, title);
    }

    private Inventory createMenu(MenuType type, int page, int size, Component title) {
        return createMenu(type, null, page, size, title);
    }

    private Inventory createMenu(MenuType type, String context, int page, int size, Component title) {
        return MenuHolder.createInventory(new MenuHolder(type, context, page), size, title);
    }

    // ========== MAIN MENU (27 slots, 3 rows) ==========

    public void openMainMenu(Player player) {
        Inventory inv = createMenu(MenuType.MAIN, 27,
                Component.text(MAIN_MENU_TITLE, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        ItemStack pane = createPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        // Slot 10: Diamond Sword — Duels
        if (plugin.isFeatureEnabled("challenges")) {
            inv.setItem(10, createItem(Material.DIAMOND_SWORD, "&a&lDuels",
                    "&7Challenge another player", "&7to a private duel!"));
        } else {
            inv.setItem(10, createItem(Material.BARRIER, "&8Duels",
                    "&cDisabled on this server."));
        }

        // Slot 13: Player Head — Stats
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(player);
        headMeta.displayName(Component.text("Stats", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        headMeta.lore(List.of(
                Component.text("View your duel statistics", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        head.setItemMeta(headMeta);
        inv.setItem(13, head);

        // Slot 4: Ender Eye — Spectate
        if (plugin.isFeatureEnabled("spectating")) {
            inv.setItem(4, createItem(Material.ENDER_EYE, "&3&lSpectate",
                    "&7Watch the duels that are", "&7being fought right now!"));
        } else {
            inv.setItem(4, createItem(Material.BARRIER, "&8Spectate",
                    "&cDisabled on this server."));
        }

        // Slot 22: Anvil — Kit Editor
        if (plugin.isFeatureEnabled("kit-editor")) {
            inv.setItem(22, createItem(Material.ANVIL, "&e&lKit Editor",
                    "&7Personalize the item layout", "&7of any kit — just for you!"));
        } else {
            inv.setItem(22, createItem(Material.BARRIER, "&8Kit Editor",
                    "&cDisabled on this server."));
        }

        // Slot 16: Hopper — Matchmaking
        if (plugin.isFeatureEnabled("matchmaking")) {
            inv.setItem(16, createItem(Material.HOPPER, "&b&lMatchmaking",
                    "&7Join a queue to find", "&7an opponent automatically!"));
        } else {
            inv.setItem(16, createItem(Material.BARRIER, "&8Matchmaking",
                    "&cDisabled on this server."));
        }

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== DUELS — PLAYER SELECT (paginated, 54 slots) ==========

    public void openDuelsMenu(Player player, int page) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);
        online.removeIf(p -> plugin.getDuelManager().isBusy(p.getUniqueId())
                || plugin.getDuelManager().hasRequestsDisabled(p.getUniqueId()));

        int totalPages = Math.max(1, (int) Math.ceil((double) online.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.DUELS, page, 54,
                Component.text(DUELS_MENU_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));
        fillBorder(inv, Material.ORANGE_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < online.size(); i++) {
            Player target = online.get(start + i);
            ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) headItem.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(Component.text(target.getName(), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to challenge", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            headItem.setItemMeta(meta);
            MenuKeys.tag(headItem, MenuKeys.PLAYER, target.getUniqueId().toString());
            inv.setItem(ITEM_SLOTS[i], headItem);
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== STATS MENU (27 slots) ==========

    public void openStatsMenu(Player player) {
        Inventory inv = createMenu(MenuType.STATS, 27,
                Component.text(STATS_MENU_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));

        ItemStack pane = createPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        PlayerStats stats = plugin.getStatsManager().getStats(player.getUniqueId());

        // Slot 4: Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        headMeta.setOwningPlayer(player);
        headMeta.displayName(Component.text(player.getName(), NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        headMeta.lore(List.of(
                Component.text("Your Duel Profile", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        head.setItemMeta(headMeta);
        inv.setItem(4, head);

        // Slot 10: Wins
        inv.setItem(10, createItem(Material.EMERALD, "&a&lWins", "&7" + stats.getWins()));

        // Slot 12: Losses
        inv.setItem(12, createItem(Material.REDSTONE, "&c&lLosses", "&7" + stats.getLosses()));

        // Slot 14: Overall
        inv.setItem(14, createItem(Material.BOOK, "&6&lOverall",
                "&7Total Duels: &f" + stats.getTotalGames(),
                "&7Win Rate: &e" + String.format("%.1f%%", stats.getWinRate())));

        // Slot 16: Score
        int score = dev.epicduels.manager.StatsManager.calculateScore(stats);
        inv.setItem(16, createItem(Material.NETHER_STAR, "&e&lScore", "&7" + score,
                "&8(wins" + "\u00b2" + " / (wins + losses))"));

        // Slot 22: Back
        inv.setItem(22, createItem(Material.ARROW, "&7Back", "&7Return to main menu"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== MATCHMAKING MENU (paginated, 54 slots) ==========

    public void openMatchmakingMenu(Player player, int page) {
        List<Kit> allKits = new ArrayList<>(plugin.getKitManager().getAllKits());

        int totalPages = Math.max(1, (int) Math.ceil((double) allKits.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.MATCHMAKING, page, 54,
                Component.text(MATCHMAKING_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < allKits.size(); i++) {
            Kit kit = allKits.get(start + i);
            int queueCount = plugin.getQueueManager().getQueueSize(kit.getName());
            boolean queued = plugin.getQueueManager().isInQueue(player.getUniqueId())
                    && kit.getName().equalsIgnoreCase(plugin.getQueueManager().getQueuedKit(player.getUniqueId()));

            String status = queued ? "&aQueued!" : "&7" + queueCount + " in queue";
            String action = queued ? "&eClick to leave queue" : "&eClick to join queue";
            inv.setItem(ITEM_SLOTS[i], MenuKeys.tag(
                    createItem(kit.getDisplayIcon(), "&b" + kit.getName(), status, action),
                    MenuKeys.KIT, kit.getName()));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== KIT SELECT — challenge flow (paginated) ==========

    public void openKitSelect(Player player, UUID targetPlayer, int page) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);

        // Build combined item list: kits + the "Own Inventory" option (if enabled)
        List<ItemStack> items = new ArrayList<>();
        for (Kit kit : plugin.getKitManager().getAllKits()) {
            items.add(MenuKeys.tag(createItem(kit.getDisplayIcon(), "&b" + kit.getName(),
                    "&7Click to select this kit"), MenuKeys.KIT, kit.getName()));
        }
        if (plugin.isOwnInventoryDuelsEnabled()) {
            items.add(MenuKeys.tag(createItem(Material.CHEST, "&6&l" + Kit.OWN_INVENTORY_DISPLAY,
                    "&7Fight with the items you", "&7are carrying right now!",
                    "&7Your inventory is saved and", "&7restored after the duel."),
                    MenuKeys.ACTION, "own-inventory"));
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.KIT_SELECT, page, 54,
                Component.text(KIT_SELECT_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < items.size(); i++) {
            inv.setItem(ITEM_SLOTS[i], items.get(start + i));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openKitSelect(Player player, UUID targetPlayer) {
        openKitSelect(player, targetPlayer, 0);
    }

    // ========== ROUNDS SELECT — challenge flow (Bo1/Bo3/Bo5) ==========

    public void openRoundsSelect(Player player, UUID targetPlayer, String kitName) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);
        challengeKit.put(player.getUniqueId(), kitName);

        Inventory inv = createMenu(MenuType.ROUNDS_SELECT, 27,
                Component.text(ROUNDS_SELECT_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));
        ItemStack pane = createPane(Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(11, createItem(Material.PAPER, "&a&lBest of 1",
                "&7A single round decides", "&7the duel.", "", "&eClick to select"));
        inv.setItem(13, createItem(Material.BOOK, "&6&lBest of 3",
                "&7First to 2 round wins", "&7takes the match.", "", "&eClick to select"));
        inv.setItem(15, createItem(Material.ENCHANTED_BOOK, "&d&lBest of 5",
                "&7First to 3 round wins", "&7takes the match.", "", "&eClick to select"));
        inv.setItem(22, createItem(Material.ARROW, "&7Back", "&7Return to kit selection"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void setChallengeRounds(UUID player, int rounds) {
        challengeRounds.put(player, rounds);
    }

    // ========== ARENA / MAP SELECT — challenge flow (paginated) ==========

    public void openArenaSelect(Player player, UUID targetPlayer, String kitName, int page) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);
        challengeKit.put(player.getUniqueId(), kitName);

        List<Arena> readyArenas = plugin.getArenaManager().getReadyArenas();
        // +1 for the "Random Map" entry
        int totalItems = readyArenas.size() + 1;
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.ARENA_SELECT, page, 54,
                Component.text(ARENA_SELECT_TITLE, NamedTextColor.GREEN, TextDecoration.BOLD));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        // Build combined item list: arenas + Random Map compass
        List<ItemStack> items = new ArrayList<>();
        for (Arena arena : readyArenas) {
            items.add(MenuKeys.tag(createItem(arena.getDisplayIcon(), "&a" + arena.getName(),
                    "&7Click to select this map"), MenuKeys.ARENA, arena.getName()));
        }
        items.add(MenuKeys.tag(createItem(Material.COMPASS, "&e&lRandom Map", "&7Randomly picks a map",
                "&7with a fun animation!"), MenuKeys.ACTION, "random-map"));

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < items.size(); i++) {
            inv.setItem(ITEM_SLOTS[i], items.get(start + i));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openArenaSelect(Player player, UUID targetPlayer, String kitName) {
        openArenaSelect(player, targetPlayer, kitName, 0);
    }

    // ========== RANDOM MAP ANIMATION ==========

    public void startRandomMapAnimation(Player player) {
        List<Arena> readyArenas = plugin.getArenaManager().getReadyArenas();
        if (readyArenas.isEmpty()) {
            Messages.send(player, "general.no-arenas");
            player.closeInventory();
            return;
        }

        animatingPlayers.add(player.getUniqueId());
        Arena chosenArena = readyArenas.get(new Random().nextInt(readyArenas.size()));

        Inventory inv = player.getOpenInventory().getTopInventory();
        int animSlot = 22; // center of rows 1-4

        new BukkitRunnable() {
            int ticks = 0;
            int currentDelay = 2;
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

                    inv.setItem(animSlot, createItem(displayArena.getDisplayIcon(),
                            "&e" + displayArena.getName(), "&7Selecting..."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.5f);
                    ticks++;

                    if (ticks > 15) currentDelay = 3;
                    if (ticks > 22) currentDelay = 4;
                    if (ticks > 27) currentDelay = 6;
                    if (ticks > 30) currentDelay = 8;
                    if (ticks > 33) currentDelay = 12;

                    if (ticks > 35) {
                        inv.setItem(animSlot, createItem(chosenArena.getDisplayIcon(),
                                "&a&l" + chosenArena.getName(), "&aSelected!"));
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                        // Keep the player marked as animating until the delayed callback runs,
                        // so a menu close in the meantime can still cancel the finish via cancelAnimation().

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            boolean stillAnimating = animatingPlayers.remove(player.getUniqueId());
                            if (stillAnimating && player.isOnline()) {
                                player.closeInventory();
                                finishChallengeWithArena(player, chosenArena.getName());
                            }
                        }, 30L);
                        cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    // ========== KIT EDIT / PREVIEW ==========

    public void openKitEdit(Player player, Kit kit) {
        Inventory inv = createMenu(MenuType.KIT_EDIT, kit.getName(), 0, 54,
                Component.text(KIT_EDIT_TITLE + kit.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));

        ItemStack[] contents = kit.getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) inv.setItem(i, contents[i].clone());
        }
        if (kit.getArmorContents() != null) {
            ItemStack[] armor = kit.getArmorContents();
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null) inv.setItem(36 + i, armor[i].clone());
            }
        }
        if (kit.getOffHand() != null) inv.setItem(40, kit.getOffHand().clone());

        // Slots 41-52 are unused by the kit (only 0-40 are saved) — block them off
        ItemStack pane = createPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 41; i <= 52; i++) {
            inv.setItem(i, pane);
        }

        inv.setItem(53, createItem(Material.EMERALD, "&aSave Kit", "&7Click to save changes"));

        player.openInventory(inv);
    }

    public void openKitPreview(Player player, Kit kit) {
        Inventory inv = createMenu(MenuType.KIT_PREVIEW, kit.getName(), 0, 54,
                Component.text(KIT_PREVIEW_TITLE + kit.getName(), NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        fillBorder(inv, Material.GRAY_STAINED_GLASS_PANE);

        ItemStack[] contents = kit.getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) inv.setItem(i + 9, contents[i].clone());
        }
        if (kit.getArmorContents() != null) {
            ItemStack[] armor = kit.getArmorContents();
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null) inv.setItem(45 + i, armor[i].clone());
            }
        }
        if (kit.getOffHand() != null) inv.setItem(49, kit.getOffHand().clone());

        player.openInventory(inv);
    }

    // ========== KIT EDITOR (per-player personalization) ==========

    public void openKitEditorList(Player player, int page) {
        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int totalPages = Math.max(1, (int) Math.ceil((double) kits.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.KIT_EDITOR_LIST, page, 54,
                Component.text(KIT_EDITOR_LIST_TITLE, NamedTextColor.YELLOW, TextDecoration.BOLD));
        fillBorder(inv, Material.YELLOW_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < kits.size(); i++) {
            Kit kit = kits.get(start + i);
            boolean customized = plugin.getPlayerKitManager().hasLayout(player.getUniqueId(), kit.getName());
            String status = customized ? "&aPersonalized layout" : "&7Default layout";
            inv.setItem(ITEM_SLOTS[i], MenuKeys.tag(createItem(kit.getDisplayIcon(), "&e" + kit.getName(),
                    status, "", "&eClick to customize"), MenuKeys.KIT, kit.getName()));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    /**
     * Opens the per-player customize GUI for a kit. Shows the player's saved
     * layout if one exists (and still matches the kit), otherwise the default.
     * Same slot mapping as the admin edit GUI: 0-35 inventory, 36-39 armor,
     * 40 offhand.
     */
    public void openKitCustomize(Player player, Kit kit) {
        Kit shown = plugin.getPlayerKitManager().getPersonalizedKit(player.getUniqueId(), kit);

        Inventory inv = createMenu(MenuType.KIT_CUSTOMIZE, kit.getName(), 0, 54,
                Component.text(KIT_CUSTOMIZE_TITLE + kit.getName(), NamedTextColor.YELLOW, TextDecoration.BOLD));

        ItemStack[] contents = shown.getContents();
        for (int i = 0; i < Math.min(contents.length, 36); i++) {
            if (contents[i] != null) inv.setItem(i, contents[i].clone());
        }
        if (shown.getArmorContents() != null) {
            ItemStack[] armor = shown.getArmorContents();
            for (int i = 0; i < Math.min(armor.length, 4); i++) {
                if (armor[i] != null) inv.setItem(36 + i, armor[i].clone());
            }
        }
        if (shown.getOffHand() != null) inv.setItem(40, shown.getOffHand().clone());

        ItemStack pane = createPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 41; i <= 52; i++) {
            inv.setItem(i, pane);
        }

        inv.setItem(CUSTOMIZE_INFO_SLOT, createItem(Material.BOOK, "&e&lHow it works",
                "&7Rearrange the kit's items into",
                "&7the slots YOU prefer.",
                "&7Rows 1-4: inventory (row 1 = hotbar).",
                "&7Bottom row: boots, leggings,",
                "&7chestplate, helmet, offhand.",
                "",
                "&7You cannot add or remove items."));
        inv.setItem(CUSTOMIZE_RESET_SLOT, createItem(Material.TNT, "&cReset to Default",
                "&7Delete your personalized layout", "&7and restore the kit's default."));
        inv.setItem(CUSTOMIZE_SAVE_SLOT, createItem(Material.EMERALD, "&aSave Layout",
                "&7Save this arrangement.", "&7It is applied whenever you", "&7duel with this kit."));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== SPECTATE — LIVE DUELS (paginated) ==========

    public void openSpectateMenu(Player player, int page) {
        List<dev.epicduels.model.DuelInstance> duels =
                new ArrayList<>(plugin.getDuelManager().getActiveDuels());

        int totalPages = Math.max(1, (int) Math.ceil((double) duels.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.SPECTATE, page, 54,
                Component.text(SPECTATE_TITLE, NamedTextColor.DARK_AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.LIGHT_BLUE_STAINED_GLASS_PANE);

        if (duels.isEmpty()) {
            inv.setItem(22, createItem(Material.CLOCK, "&7No duels running",
                    "&7Nobody is fighting right now.", "&7Check back later!"));
        }

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < duels.size(); i++) {
            dev.epicduels.model.DuelInstance duel = duels.get(start + i);
            Player p1 = Bukkit.getPlayer(duel.getPlayer1());
            Player p2 = Bukkit.getPlayer(duel.getPlayer2());
            String name1 = p1 != null ? p1.getName() : "?";
            String name2 = p2 != null ? p2.getName() : "?";

            List<String> loreLines = new ArrayList<>();
            loreLines.add("&7Kit: &f" + Kit.displayName(duel.getKitName()));
            loreLines.add("&7Map: &f" + duel.getArenaName());
            if (duel.getBestOf() > 1) {
                loreLines.add("&7Round &f" + duel.getCurrentRound() + " &7of Best of " + duel.getBestOf());
            }
            loreLines.add("&7Duration: &f" + formatDuration(duel.getStartMillis()));
            loreLines.add("&eClick to spectate");

            ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) headItem.getItemMeta();
            if (p1 != null) meta.setOwningPlayer(p1);
            meta.displayName(colorize("&a" + name1 + " &7vs &a" + name2)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            for (String line : loreLines) {
                lore.add(colorize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            headItem.setItemMeta(meta);
            MenuKeys.tag(headItem, MenuKeys.PLAYER, duel.getPlayer1().toString());

            inv.setItem(ITEM_SLOTS[i], headItem);
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    private String formatDuration(long startMillis) {
        if (startMillis <= 0) return "starting...";
        long seconds = Math.max(0, (System.currentTimeMillis() - startMillis) / 1000);
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    // ========== KIT LIST / ARENA LIST (paginated) ==========

    public void openKitList(Player player, int page) {
        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int totalPages = Math.max(1, (int) Math.ceil((double) kits.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.KIT_LIST, page, 54,
                Component.text(KIT_LIST_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < kits.size(); i++) {
            Kit kit = kits.get(start + i);
            inv.setItem(ITEM_SLOTS[i], MenuKeys.tag(
                    createItem(kit.getDisplayIcon(), "&b" + kit.getName(), "&7Click to preview"),
                    MenuKeys.KIT, kit.getName()));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openArenaList(Player player, int page) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getAllArenas());
        int totalPages = Math.max(1, (int) Math.ceil((double) arenas.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.ARENA_LIST, page, 54,
                Component.text(ARENA_LIST_TITLE, NamedTextColor.GREEN, TextDecoration.BOLD));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < arenas.size(); i++) {
            Arena arena = arenas.get(start + i);
            String status = arena.isReady() ? "&aReady" : "&cIncomplete";
            inv.setItem(ITEM_SLOTS[i], createItem(arena.getDisplayIcon(), "&a" + arena.getName(), status));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== Challenge flow helpers ==========

    public void finishChallengeWithArena(Player player, String arenaName) {
        UUID targetUUID = challengeTarget.get(player.getUniqueId());
        String kitName = challengeKit.get(player.getUniqueId());
        int bestOf = challengeRounds.getOrDefault(player.getUniqueId(), 1);
        clearChallengeData(player.getUniqueId());

        if (targetUUID == null || kitName == null) return;

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            Messages.send(player, "general.player-offline");
            return;
        }

        if (plugin.getDuelManager().hasRequestsDisabled(targetUUID)) {
            Messages.send(player, "challenge.requests-disabled");
            return;
        }

        boolean sent = plugin.getDuelManager().sendRequest(player.getUniqueId(), targetUUID, arenaName, kitName, bestOf);
        if (!sent) {
            Messages.send(player, "challenge.send-failed");
            return;
        }

        String kitDisplay = Kit.displayName(kitName);
        Messages.send(player, "challenge.sent", Messages.unparsed("player", target.getName()));
        Messages.send(player, bestOf > 1 ? "challenge.sent-details-bestof" : "challenge.sent-details",
                Messages.unparsed("arena", arenaName), Messages.unparsed("kit", kitDisplay),
                Messages.unparsed("rounds", bestOf));

        target.sendMessage(Component.empty());
        Messages.send(target, "general.separator");
        Messages.send(target, "challenge.received", Messages.unparsed("player", player.getName()));
        Messages.send(target, bestOf > 1 ? "challenge.received-details-bestof" : "challenge.received-details",
                Messages.unparsed("arena", arenaName), Messages.unparsed("kit", kitDisplay),
                Messages.unparsed("rounds", bestOf));
        target.sendMessage(Messages.format("challenge.accept-deny", Map.of("player", player.getName())));
        Messages.send(target, "challenge.expires");
        Messages.send(target, "general.separator");
        target.sendMessage(Component.empty());

        target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    public UUID getChallengeTarget(UUID player) {
        return challengeTarget.get(player);
    }

    public String getChallengeKit(UUID player) {
        return challengeKit.get(player);
    }

    public void clearChallengeData(UUID player) {
        challengeTarget.remove(player);
        challengeKit.remove(player);
        challengeRounds.remove(player);
        animatingPlayers.remove(player);
        partyFlowMode.remove(player);
        partyFlowTeamSize.remove(player);
        partyFlowKit.remove(player);
    }

    // ========== PARTY FLOW GUIs ==========

    public void openPartyModeMenu(Player owner) {
        partyFlowMode.remove(owner.getUniqueId());
        partyFlowTeamSize.remove(owner.getUniqueId());
        partyFlowKit.remove(owner.getUniqueId());

        Inventory inv = createMenu(MenuType.PARTY_MODE, 27,
                Component.text(PARTY_MODE_TITLE, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        ItemStack pane = createPane(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        if (plugin.isFeatureEnabled("team-duels")) {
            inv.setItem(11, createItem(Material.DIAMOND_SWORD, "&9&lTeam Duel",
                    "&7Split your party into two teams", "&7and fight on a normal arena.",
                    "", "&eClick to choose"));
        } else {
            inv.setItem(11, createItem(Material.BARRIER, "&8Team Duel",
                    "&cDisabled on this server."));
        }
        if (plugin.isFeatureEnabled("tournaments")) {
            inv.setItem(15, createItem(Material.GOLDEN_HORSE_ARMOR, "&6&lTournament",
                    "&7Single-elimination 1v1 bracket", "&7with all party members.",
                    "", "&eClick to choose"));
        } else {
            inv.setItem(15, createItem(Material.BARRIER, "&8Tournament",
                    "&cDisabled on this server."));
        }

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openPartyTeamSizeMenu(Player owner) {
        partyFlowMode.put(owner.getUniqueId(), dev.epicduels.model.PartyMode.TEAM_DUEL);

        Inventory inv = createMenu(MenuType.PARTY_TEAM_SIZE, 27,
                Component.text(PARTY_TEAM_SIZE_TITLE, NamedTextColor.BLUE, TextDecoration.BOLD));
        ItemStack pane = createPane(Material.BLUE_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        int partySize = 0;
        dev.epicduels.model.Party party = plugin.getPartyManager().getPartyOf(owner.getUniqueId());
        if (party != null) partySize = party.size();

        inv.setItem(11, sizeOption(Material.IRON_SWORD, "2v2", 4, partySize));
        inv.setItem(13, sizeOption(Material.GOLDEN_SWORD, "3v3", 6, partySize));
        inv.setItem(15, sizeOption(Material.DIAMOND_SWORD, "4v4", 8, partySize));
        inv.setItem(22, createItem(Material.ARROW, "&7Back", "&7Return to mode selection"));

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    private ItemStack sizeOption(Material mat, String label, int required, int partySize) {
        if (partySize >= required) {
            return createItem(mat, "&a&l" + label, "&7Requires " + required + " players",
                    "&aYour party: " + partySize + " players", "", "&eClick to select");
        } else {
            return createItem(Material.BARRIER, "&c&l" + label,
                    "&7Requires " + required + " players",
                    "&cYour party: " + partySize + " players");
        }
    }

    public void openPartyKitMenu(Player owner, int page) {
        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int totalPages = Math.max(1, (int) Math.ceil((double) kits.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);

        Inventory inv = createMenu(MenuType.PARTY_KIT, page, 54,
                Component.text(PARTY_KIT_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < kits.size(); i++) {
            Kit kit = kits.get(start + i);
            inv.setItem(ITEM_SLOTS[i], MenuKeys.tag(createItem(kit.getDisplayIcon(), "&b" + kit.getName(),
                    "&7Click to select this kit"), MenuKeys.KIT, kit.getName()));
        }
        addNavigation(inv, page, totalPages);
        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openPartyConfirmMenu(Player owner) {
        dev.epicduels.model.PartyMode mode = partyFlowMode.get(owner.getUniqueId());
        String kitName = partyFlowKit.get(owner.getUniqueId());
        dev.epicduels.model.TeamSize size = partyFlowTeamSize.get(owner.getUniqueId());
        dev.epicduels.model.Party party = plugin.getPartyManager().getPartyOf(owner.getUniqueId());

        Inventory inv = createMenu(MenuType.PARTY_CONFIRM, 27,
                Component.text(PARTY_CONFIRM_TITLE, NamedTextColor.GREEN, TextDecoration.BOLD));
        ItemStack pane = createPane(Material.LIME_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        String modeLabel = mode == dev.epicduels.model.PartyMode.TOURNAMENT
                ? "Tournament" : ("Team Duel " + (size != null ? size.getLabel() : ""));
        int partySize = party != null ? party.size() : 0;

        inv.setItem(13, createItem(Material.PAPER, "&a&l" + modeLabel,
                "&7Kit: &f" + (kitName != null ? kitName : "?"),
                "&7Players: &f" + partySize));

        inv.setItem(11, createItem(Material.LIME_WOOL, "&a&lConfirm & Start",
                "&7Click to launch!"));
        inv.setItem(15, createItem(Material.RED_WOOL, "&c&lCancel",
                "&7Close this menu"));

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void setPartyFlowMode(UUID owner, dev.epicduels.model.PartyMode mode) {
        partyFlowMode.put(owner, mode);
    }

    public void setPartyFlowTeamSize(UUID owner, dev.epicduels.model.TeamSize size) {
        partyFlowTeamSize.put(owner, size);
    }

    public void setPartyFlowKit(UUID owner, String kitName) {
        partyFlowKit.put(owner, kitName);
    }

    public dev.epicduels.model.PartyMode getPartyFlowMode(UUID owner) {
        return partyFlowMode.get(owner);
    }

    public dev.epicduels.model.TeamSize getPartyFlowTeamSize(UUID owner) {
        return partyFlowTeamSize.get(owner);
    }

    public String getPartyFlowKit(UUID owner) {
        return partyFlowKit.get(owner);
    }

    public void clearPartyFlow(UUID owner) {
        partyFlowMode.remove(owner);
        partyFlowTeamSize.remove(owner);
        partyFlowKit.remove(owner);
    }

    public boolean isAnimating(UUID player) {
        return animatingPlayers.contains(player);
    }

    /**
     * Cancels a running "Random Map" animation for a player, if any.
     * The running animation task checks {@code animatingPlayers} every tick and
     * stops itself (without finishing the challenge) once the entry is removed.
     */
    public void cancelAnimation(UUID player) {
        animatingPlayers.remove(player);
    }

    // ========== Navigation & Utility ==========

    private void addNavigation(Inventory inv, int page, int totalPages) {
        ItemStack navPane = createPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, navPane);
        }

        if (page > 0) {
            inv.setItem(PREV_SLOT, createItem(Material.ARROW, "&ePrevious Page",
                    "&7Page " + page + "/" + totalPages));
        }

        inv.setItem(BACK_SLOT, createItem(Material.BARRIER, "&cBack", "&7Return to previous menu"));

        if (page < totalPages - 1) {
            inv.setItem(NEXT_SLOT, createItem(Material.ARROW, "&eNext Page",
                    "&7Page " + (page + 2) + "/" + totalPages));
        }
    }

    private int clampPage(int page, int totalPages) {
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    private void fillBorder(Inventory inv, Material pane) {
        ItemStack border = createPane(pane);
        int size = inv.getSize();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = size - 9; i < size; i++) inv.setItem(i, border);
        for (int i = 9; i < size - 9; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
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
