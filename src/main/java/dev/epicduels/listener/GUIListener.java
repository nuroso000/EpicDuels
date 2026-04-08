package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import dev.epicduels.manager.GUIManager;
import dev.epicduels.model.Arena;
import dev.epicduels.model.Kit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class GUIListener implements Listener {

    private final EpicDuels plugin;

    public GUIListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = getInventoryTitle(event.getView().title());
        if (title == null) return;

        // Kit edit is the only non-read-only GUI
        if (title.startsWith(GUIManager.KIT_EDIT_TITLE)) {
            handleKitEditClick(event, player, title);
            return;
        }

        // Check if this is one of our read-only GUIs
        if (!isOurGUI(title)) return;

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (clicked.getType().name().endsWith("STAINED_GLASS_PANE")) return;

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        int slot = event.getSlot();

        switch (title) {
            case GUIManager.MAIN_MENU_TITLE -> handleMainMenuClick(player, slot);
            case GUIManager.DUELS_MENU_TITLE -> handleDuelsMenuClick(player, slot, clicked);
            case GUIManager.STATS_MENU_TITLE -> handleStatsMenuClick(player, slot);
            case GUIManager.MATCHMAKING_TITLE -> handleMatchmakingClick(player, slot, clicked);
            case GUIManager.KIT_SELECT_TITLE -> handleKitSelectClick(player, slot, clicked);
            case GUIManager.ARENA_SELECT_TITLE -> handleArenaSelectClick(player, slot, clicked);
            case GUIManager.KIT_LIST_TITLE -> handleKitListClick(player, slot, clicked);
            default -> {
                if (title.startsWith(GUIManager.KIT_PREVIEW_TITLE)) {
                    // Preview is view-only, no action
                }
            }
        }
    }

    // ========== MAIN MENU ==========

    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 10 -> { // Diamond Sword — Duels
                player.closeInventory();
                plugin.getGUIManager().openDuelsMenu(player, 0);
            }
            case 13 -> { // Player Head — Stats
                player.closeInventory();
                plugin.getGUIManager().openStatsMenu(player);
            }
            case 16 -> { // Hopper — Matchmaking
                player.closeInventory();
                plugin.getGUIManager().openMatchmakingMenu(player, 0);
            }
        }
    }

    // ========== DUELS — PLAYER SELECT ==========

    private void handleDuelsMenuClick(Player player, int slot, ItemStack clicked) {
        // Navigation
        if (handlePagination(player, slot, GUIManager.DUELS_MENU_TITLE)) return;

        // Back button
        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        // Player head click
        if (clicked.getType() != Material.PLAYER_HEAD) return;
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        if (meta.getOwningPlayer() == null) return;

        Player target = meta.getOwningPlayer().getPlayer();
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player is no longer online.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        if (plugin.getDuelManager().isInDuel(target.getUniqueId())) {
            player.sendMessage(Component.text("That player is already in a duel!", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        player.closeInventory();
        plugin.getGUIManager().openKitSelect(player, target.getUniqueId());
    }

    // ========== STATS ==========

    private void handleStatsMenuClick(Player player, int slot) {
        if (slot == 22) { // Back arrow
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
        }
    }

    // ========== MATCHMAKING ==========

    private void handleMatchmakingClick(Player player, int slot, ItemStack clicked) {
        if (handlePagination(player, slot, GUIManager.MATCHMAKING_TITLE)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        // Kit item click — toggle queue
        String itemName = getItemName(clicked);
        if (itemName == null) return;

        Kit kit = plugin.getKitManager().getKit(itemName);
        if (kit == null) return;

        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a duel!", NamedTextColor.RED));
            return;
        }

        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId());
            player.sendMessage(Component.text("You left the queue.", NamedTextColor.YELLOW));
            player.sendActionBar(Component.empty());
        } else {
            boolean joined = plugin.getQueueManager().joinQueue(player.getUniqueId(), kit.getName());
            if (joined) {
                player.sendMessage(Component.text("You joined the queue for: " + kit.getName(), NamedTextColor.GREEN));
            } else {
                player.sendMessage(Component.text("Could not join queue.", NamedTextColor.RED));
            }
        }

        // Refresh the menu to show updated queue status
        player.closeInventory();
        plugin.getGUIManager().openMatchmakingMenu(player,
                plugin.getGUIManager().getPlayerPage(player.getUniqueId()));
    }

    // ========== KIT SELECT (challenge flow) ==========

    private void handleKitSelectClick(Player player, int slot, ItemStack clicked) {
        if (handlePagination(player, slot, GUIManager.KIT_SELECT_TITLE)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            plugin.getGUIManager().openDuelsMenu(player, 0);
            return;
        }

        String itemName = getItemName(clicked);
        if (itemName == null) return;

        Kit kit = plugin.getKitManager().getKit(itemName);
        if (kit == null) {
            player.sendMessage(Component.text("Kit not available.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        UUID targetUUID = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
        if (targetUUID == null) {
            player.closeInventory();
            return;
        }

        player.closeInventory();
        plugin.getGUIManager().openArenaSelect(player, targetUUID, kit.getName());
    }

    // ========== ARENA SELECT (challenge flow) ==========

    private void handleArenaSelectClick(Player player, int slot, ItemStack clicked) {
        if (handlePagination(player, slot, GUIManager.ARENA_SELECT_TITLE)) return;

        if (slot == GUIManager.BACK_SLOT) {
            UUID targetUUID = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
            player.closeInventory();
            if (targetUUID != null) {
                plugin.getGUIManager().openKitSelect(player, targetUUID);
            } else {
                plugin.getGUIManager().openDuelsMenu(player, 0);
            }
            return;
        }

        // Random Map compass
        if (clicked.getType() == Material.COMPASS) {
            String itemName = getItemName(clicked);
            if (itemName != null && itemName.equals("Random Map")) {
                if (!plugin.getGUIManager().isAnimating(player.getUniqueId())) {
                    plugin.getGUIManager().startRandomMapAnimation(player);
                }
                return;
            }
        }

        if (plugin.getGUIManager().isAnimating(player.getUniqueId())) return;

        String itemName = getItemName(clicked);
        if (itemName == null) return;

        Arena arena = plugin.getArenaManager().getArena(itemName);
        if (arena == null || !arena.isReady()) {
            player.sendMessage(Component.text("Arena not available.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        player.closeInventory();
        plugin.getGUIManager().finishChallengeWithArena(player, arena.getName());
    }

    // ========== KIT LIST (admin) ==========

    private void handleKitListClick(Player player, int slot, ItemStack clicked) {
        if (handlePagination(player, slot, GUIManager.KIT_LIST_TITLE)) return;

        if (slot == GUIManager.BACK_SLOT) {
            player.closeInventory();
            return;
        }

        String itemName = getItemName(clicked);
        if (itemName == null) return;

        Kit kit = plugin.getKitManager().getKit(itemName);
        if (kit == null) return;

        player.closeInventory();
        plugin.getGUIManager().openKitPreview(player, kit);
    }

    // ========== KIT EDIT ==========

    private void handleKitEditClick(InventoryClickEvent event, Player player, String title) {
        int slot = event.getRawSlot();

        if (slot == 53) {
            event.setCancelled(true);

            String kitName = title.substring(GUIManager.KIT_EDIT_TITLE.length());
            Kit kit = plugin.getKitManager().getKit(kitName);
            if (kit == null) {
                player.sendMessage(Component.text("Kit not found.", NamedTextColor.RED));
                player.closeInventory();
                return;
            }

            Inventory inv = event.getInventory();

            ItemStack[] contents = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                contents[i] = item != null ? item.clone() : null;
            }

            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                ItemStack item = inv.getItem(36 + i);
                armor[i] = item != null ? item.clone() : null;
            }

            ItemStack offHandItem = inv.getItem(40);
            ItemStack offHand = offHandItem != null ? offHandItem.clone() : null;

            kit.setContents(contents);
            kit.setArmorContents(armor);
            kit.setOffHand(offHand);
            plugin.getKitManager().updateKit(kit);

            player.closeInventory();
            player.sendMessage(Component.text("Kit '" + kitName + "' saved!", NamedTextColor.GREEN));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        }
    }

    // ========== PAGINATION ==========

    /**
     * Handles prev/next page clicks. Returns true if a nav button was clicked.
     */
    private boolean handlePagination(Player player, int slot, String menuTitle) {
        int currentPage = plugin.getGUIManager().getPlayerPage(player.getUniqueId());

        if (slot == GUIManager.PREV_SLOT) {
            if (currentPage > 0) {
                player.closeInventory();
                openMenuAtPage(player, menuTitle, currentPage - 1);
            }
            return true;
        }

        if (slot == GUIManager.NEXT_SLOT) {
            player.closeInventory();
            openMenuAtPage(player, menuTitle, currentPage + 1);
            return true;
        }

        return false;
    }

    private void openMenuAtPage(Player player, String menuTitle, int page) {
        switch (menuTitle) {
            case GUIManager.DUELS_MENU_TITLE -> plugin.getGUIManager().openDuelsMenu(player, page);
            case GUIManager.MATCHMAKING_TITLE -> plugin.getGUIManager().openMatchmakingMenu(player, page);
            case GUIManager.KIT_SELECT_TITLE -> {
                UUID target = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
                if (target != null) {
                    plugin.getGUIManager().openKitSelect(player, target, page);
                }
            }
            case GUIManager.ARENA_SELECT_TITLE -> {
                UUID target = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
                String kit = plugin.getGUIManager().getChallengeKit(player.getUniqueId());
                if (target != null && kit != null) {
                    plugin.getGUIManager().openArenaSelect(player, target, kit, page);
                }
            }
            case GUIManager.KIT_LIST_TITLE -> plugin.getGUIManager().openKitList(player, page);
            case GUIManager.ARENA_LIST_TITLE -> plugin.getGUIManager().openArenaList(player, page);
        }
    }

    // ========== Utility ==========

    private boolean isOurGUI(String title) {
        return title.equals(GUIManager.MAIN_MENU_TITLE)
                || title.equals(GUIManager.DUELS_MENU_TITLE)
                || title.equals(GUIManager.STATS_MENU_TITLE)
                || title.equals(GUIManager.MATCHMAKING_TITLE)
                || title.equals(GUIManager.KIT_SELECT_TITLE)
                || title.equals(GUIManager.ARENA_SELECT_TITLE)
                || title.equals(GUIManager.KIT_LIST_TITLE)
                || title.equals(GUIManager.ARENA_LIST_TITLE)
                || title.startsWith(GUIManager.KIT_PREVIEW_TITLE);
    }

    private String getInventoryTitle(Component title) {
        return PlainTextComponentSerializer.plainText().serialize(title);
    }

    private String getItemName(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
    }
}
