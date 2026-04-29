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

    // GUI title constants
    public static final String MAIN_MENU_TITLE = "EpicDuels";
    public static final String DUELS_MENU_TITLE = "Duels - Select Player";
    public static final String STATS_MENU_TITLE = "Your Stats";
    public static final String MATCHMAKING_TITLE = "Matchmaking";
    public static final String KIT_SELECT_TITLE = "Select Kit";
    public static final String ARENA_SELECT_TITLE = "Select Map";
    public static final String KIT_EDIT_TITLE = "Edit Kit: ";
    public static final String KIT_PREVIEW_TITLE = "Preview Kit: ";
    public static final String KIT_LIST_TITLE = "Kits";
    public static final String ARENA_LIST_TITLE = "Arenas";
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

    // Pagination state
    private final Map<UUID, Integer> playerPage = new HashMap<>();

    // Random map animation state
    private final Set<UUID> animatingPlayers = new HashSet<>();

    // Party flow state (per owner)
    private final Map<UUID, dev.epicduels.model.PartyMode> partyFlowMode = new HashMap<>();
    private final Map<UUID, dev.epicduels.model.TeamSize> partyFlowTeamSize = new HashMap<>();
    private final Map<UUID, String> partyFlowKit = new HashMap<>();

    public GUIManager(EpicDuels plugin) {
        this.plugin = plugin;
    }

    // ========== MAIN MENU (27 slots, 3 rows) ==========

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text(MAIN_MENU_TITLE, NamedTextColor.DARK_PURPLE, TextDecoration.BOLD));

        ItemStack pane = createPane(Material.GRAY_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, pane);
        }

        // Slot 10: Diamond Sword — Duels
        inv.setItem(10, createItem(Material.DIAMOND_SWORD, "&a&lDuels",
                "&7Challenge another player", "&7to a private duel!"));

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

        // Slot 16: Hopper — Matchmaking
        inv.setItem(16, createItem(Material.HOPPER, "&b&lMatchmaking",
                "&7Join a queue to find", "&7an opponent automatically!"));

        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== DUELS — PLAYER SELECT (paginated, 54 slots) ==========

    public void openDuelsMenu(Player player, int page) {
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.remove(player);
        online.removeIf(p -> plugin.getDuelManager().isInDuel(p.getUniqueId()));

        int totalPages = Math.max(1, (int) Math.ceil((double) online.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
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
            inv.setItem(ITEM_SLOTS[i], headItem);
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== STATS MENU (27 slots) ==========

    public void openStatsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
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
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
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
            inv.setItem(ITEM_SLOTS[i], createItem(kit.getDisplayIcon(), "&b" + kit.getName(), status, action));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    // ========== KIT SELECT — challenge flow (paginated) ==========

    public void openKitSelect(Player player, UUID targetPlayer, int page) {
        challengeTarget.put(player.getUniqueId(), targetPlayer);

        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int totalPages = Math.max(1, (int) Math.ceil((double) kits.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(KIT_SELECT_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < kits.size(); i++) {
            Kit kit = kits.get(start + i);
            inv.setItem(ITEM_SLOTS[i], createItem(kit.getDisplayIcon(), "&b" + kit.getName(),
                    "&7Click to select this kit"));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openKitSelect(Player player, UUID targetPlayer) {
        openKitSelect(player, targetPlayer, 0);
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
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(ARENA_SELECT_TITLE, NamedTextColor.GREEN, TextDecoration.BOLD));
        fillBorder(inv, Material.LIME_STAINED_GLASS_PANE);

        // Build combined item list: arenas + Random Map compass
        List<ItemStack> items = new ArrayList<>();
        for (Arena arena : readyArenas) {
            items.add(createItem(arena.getDisplayIcon(), "&a" + arena.getName(), "&7Click to select this map"));
        }
        items.add(createItem(Material.COMPASS, "&e&lRandom Map", "&7Randomly picks a map",
                "&7with a fun animation!"));

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
            player.sendMessage(Component.text("No arenas available!", NamedTextColor.RED));
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
                        animatingPlayers.remove(player.getUniqueId());

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
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
        Inventory inv = Bukkit.createInventory(null, 54,
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
        inv.setItem(53, createItem(Material.EMERALD, "&aSave Kit", "&7Click to save changes"));

        player.openInventory(inv);
    }

    public void openKitPreview(Player player, Kit kit) {
        Inventory inv = Bukkit.createInventory(null, 54,
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

    // ========== KIT LIST / ARENA LIST (paginated) ==========

    public void openKitList(Player player, int page) {
        List<Kit> kits = new ArrayList<>(plugin.getKitManager().getAllKits());
        int totalPages = Math.max(1, (int) Math.ceil((double) kits.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(KIT_LIST_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < kits.size(); i++) {
            Kit kit = kits.get(start + i);
            inv.setItem(ITEM_SLOTS[i], createItem(kit.getDisplayIcon(), "&b" + kit.getName(), "&7Click to preview"));
        }

        addNavigation(inv, page, totalPages);
        player.openInventory(inv);
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openArenaList(Player player, int page) {
        List<Arena> arenas = new ArrayList<>(plugin.getArenaManager().getAllArenas());
        int totalPages = Math.max(1, (int) Math.ceil((double) arenas.size() / ITEMS_PER_PAGE));
        page = clampPage(page, totalPages);
        playerPage.put(player.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
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

    public String getChallengeKit(UUID player) {
        return challengeKit.get(player);
    }

    public void clearChallengeData(UUID player) {
        challengeTarget.remove(player);
        challengeKit.remove(player);
        animatingPlayers.remove(player);
        playerPage.remove(player);
        partyFlowMode.remove(player);
        partyFlowTeamSize.remove(player);
        partyFlowKit.remove(player);
    }

    // ========== PARTY FLOW GUIs ==========

    public void openPartyModeMenu(Player owner) {
        partyFlowMode.remove(owner.getUniqueId());
        partyFlowTeamSize.remove(owner.getUniqueId());
        partyFlowKit.remove(owner.getUniqueId());

        Inventory inv = Bukkit.createInventory(null, 27,
                Component.text(PARTY_MODE_TITLE, NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
        ItemStack pane = createPane(Material.PURPLE_STAINED_GLASS_PANE);
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(11, createItem(Material.DIAMOND_SWORD, "&9&lTeam Duel",
                "&7Split your party into two teams", "&7and fight on a normal arena.",
                "", "&eClick to choose"));
        inv.setItem(15, createItem(Material.GOLDEN_HORSE_ARMOR, "&6&lTournament",
                "&7Single-elimination 1v1 bracket", "&7with all party members.",
                "", "&eClick to choose"));

        owner.openInventory(inv);
        owner.playSound(owner.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    public void openPartyTeamSizeMenu(Player owner) {
        partyFlowMode.put(owner.getUniqueId(), dev.epicduels.model.PartyMode.TEAM_DUEL);

        Inventory inv = Bukkit.createInventory(null, 27,
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
        playerPage.put(owner.getUniqueId(), page);

        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(PARTY_KIT_TITLE, NamedTextColor.AQUA, TextDecoration.BOLD));
        fillBorder(inv, Material.CYAN_STAINED_GLASS_PANE);

        int start = page * ITEMS_PER_PAGE;
        for (int i = 0; i < ITEMS_PER_PAGE && start + i < kits.size(); i++) {
            Kit kit = kits.get(start + i);
            inv.setItem(ITEM_SLOTS[i], createItem(kit.getDisplayIcon(), "&b" + kit.getName(),
                    "&7Click to select this kit"));
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

        Inventory inv = Bukkit.createInventory(null, 27,
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

    public int getPlayerPage(UUID playerId) {
        return playerPage.getOrDefault(playerId, 0);
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
