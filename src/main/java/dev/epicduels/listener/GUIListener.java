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

        if (title.startsWith(GUIManager.KIT_EDIT_TITLE)) {
            handleKitEditClick(event, player, title);
            return;
        }

        // All other GUIs are read-only
        if (title.equals(GUIManager.MAIN_MENU_TITLE)
                || title.equals(GUIManager.ARENA_SELECT_TITLE)
                || title.equals(GUIManager.KIT_SELECT_TITLE)
                || title.startsWith(GUIManager.KIT_PREVIEW_TITLE)
                || title.equals(GUIManager.PLAYER_SELECT_TITLE)
                || title.equals(GUIManager.QUEUE_KIT_SELECT_TITLE)
                || title.equals("Kits")
                || title.equals("Arenas")) {

            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // Skip border/divider panes
            if (clicked.getType().name().endsWith("STAINED_GLASS_PANE")) return;

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

            if (title.equals(GUIManager.MAIN_MENU_TITLE)) {
                handleMainMenuClick(player, event.getSlot(), clicked);
            } else if (title.equals(GUIManager.PLAYER_SELECT_TITLE)) {
                handlePlayerSelectClick(player, clicked);
            } else if (title.equals(GUIManager.KIT_SELECT_TITLE)) {
                handleKitSelectClick(player, clicked);
            } else if (title.equals(GUIManager.ARENA_SELECT_TITLE)) {
                handleArenaSelectClick(player, clicked, event.getSlot(), event.getInventory());
            } else if (title.equals("Kits")) {
                handleKitListClick(player, clicked);
            }
        }
    }

    private void handleMainMenuClick(Player player, int slot, ItemStack clicked) {
        // LEFT SECTION: Challenge (columns 0-2)
        int col = slot % 9;
        int row = slot / 9;

        // Challenge button area (left section)
        if (col <= 2) {
            if (!player.hasPermission("epicduels.duel")) {
                player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                return;
            }
            player.closeInventory();
            plugin.getGUIManager().openPlayerSelect(player);
            return;
        }

        // RIGHT SECTION: Queue (columns 6-8)
        if (col >= 6) {
            String itemName = getItemName(clicked);
            if (itemName == null) return;

            // Handle special buttons
            if (itemName.equals("Random Kit")) {
                handleQueueToggle(player, getRandomKitName());
                return;
            }
            if (itemName.equals("No Kit")) {
                handleQueueToggle(player, "__nokit__");
                return;
            }
            if (itemName.equals("Queue / Matchmaking")) return;

            // Regular kit queue button
            Kit kit = plugin.getKitManager().getKit(itemName);
            if (kit != null) {
                handleQueueToggle(player, kit.getName());
            }
            return;
        }

        // MIDDLE SECTION: Stats (column 4) - no click action needed, stats are displayed inline
    }

    private void handleQueueToggle(Player player, String kitName) {
        if (plugin.getDuelManager().isInDuel(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a duel!", NamedTextColor.RED));
            return;
        }

        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(player.getUniqueId());
            player.sendMessage(Component.text("You left the queue.", NamedTextColor.YELLOW));
            player.sendActionBar(Component.empty());
            player.closeInventory();
            plugin.getGUIManager().openMainMenu(player);
            return;
        }

        if (kitName == null) {
            player.sendMessage(Component.text("No kits available!", NamedTextColor.RED));
            return;
        }

        boolean joined = plugin.getQueueManager().joinQueue(player.getUniqueId(), kitName);
        if (joined) {
            String displayName = kitName.equals("__nokit__") ? "No Kit" : kitName;
            player.sendMessage(Component.text("You joined the queue for: " + displayName, NamedTextColor.GREEN));
            player.closeInventory();
        } else {
            player.sendMessage(Component.text("Could not join queue. You may already be in a duel.", NamedTextColor.RED));
        }
    }

    private String getRandomKitName() {
        var kitNames = plugin.getKitManager().getKitNames();
        if (kitNames.isEmpty()) return null;
        return kitNames.get(new java.util.Random().nextInt(kitNames.size()));
    }

    private void handlePlayerSelectClick(Player player, ItemStack clicked) {
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
        // After selecting player, open kit selection (new flow: player -> kit -> map)
        plugin.getGUIManager().openKitSelect(player, target.getUniqueId());
    }

    private void handleKitSelectClick(Player player, ItemStack clicked) {
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
        // After selecting kit, open map/arena selection
        plugin.getGUIManager().openArenaSelect(player, targetUUID, kit.getName());
    }

    private void handleArenaSelectClick(Player player, ItemStack clicked, int slot, Inventory inv) {
        // Check if this is the "Random Map" button (compass in last row center)
        if (clicked.getType() == Material.COMPASS) {
            String itemName = getItemName(clicked);
            if (itemName != null && itemName.equals("Random Map")) {
                if (!plugin.getGUIManager().isAnimating(player.getUniqueId())) {
                    plugin.getGUIManager().startRandomMapAnimation(player);
                }
                return;
            }
        }

        // Don't allow clicking during animation
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

    private void handleKitListClick(Player player, ItemStack clicked) {
        String itemName = getItemName(clicked);
        if (itemName == null) return;

        Kit kit = plugin.getKitManager().getKit(itemName);
        if (kit == null) return;

        player.closeInventory();
        plugin.getGUIManager().openKitPreview(player, kit);
    }

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

    private String getInventoryTitle(Component title) {
        return PlainTextComponentSerializer.plainText().serialize(title);
    }

    private String getItemName(ItemStack item) {
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return null;
        return PlainTextComponentSerializer.plainText().serialize(item.getItemMeta().displayName());
    }
}
