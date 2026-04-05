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
import org.bukkit.event.inventory.InventoryCloseEvent;
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
                || title.equals("Kits")
                || title.equals("Arenas")) {

            event.setCancelled(true);

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            // Skip border panes
            if (clicked.getType().name().endsWith("STAINED_GLASS_PANE")) return;

            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);

            if (title.equals(GUIManager.MAIN_MENU_TITLE)) {
                handleMainMenuClick(player, event.getSlot());
            } else if (title.equals(GUIManager.PLAYER_SELECT_TITLE)) {
                handlePlayerSelectClick(player, clicked);
            } else if (title.equals(GUIManager.ARENA_SELECT_TITLE)) {
                handleArenaSelectClick(player, clicked);
            } else if (title.equals(GUIManager.KIT_SELECT_TITLE)) {
                handleKitSelectClick(player, clicked);
            } else if (title.equals("Kits")) {
                handleKitListClick(player, clicked);
            }
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        player.closeInventory();
        switch (slot) {
            case 10 -> { // Challenge Player
                if (!player.hasPermission("epicduels.duel")) {
                    player.sendMessage(Component.text("No permission.", NamedTextColor.RED));
                    return;
                }
                plugin.getGUIManager().openPlayerSelect(player);
            }
            case 12 -> { // My Stats
                player.performCommand("duel stats");
            }
            case 14 -> { // Kits
                plugin.getGUIManager().openKitList(player);
            }
            case 16 -> { // Arenas
                plugin.getGUIManager().openArenaList(player);
            }
        }
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
        plugin.getGUIManager().openArenaSelect(player, target.getUniqueId());
    }

    private void handleArenaSelectClick(Player player, ItemStack clicked) {
        String itemName = getItemName(clicked);
        if (itemName == null) return;

        Arena arena = plugin.getArenaManager().getArena(itemName);
        if (arena == null || !arena.isReady()) {
            player.sendMessage(Component.text("Arena not available.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }

        UUID target = plugin.getGUIManager().getChallengeTarget(player.getUniqueId());
        if (target == null) {
            player.closeInventory();
            return;
        }

        player.closeInventory();
        plugin.getGUIManager().openKitSelect(player, target, arena.getName());
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
        String arenaName = plugin.getGUIManager().getChallengeArena(player.getUniqueId());

        if (targetUUID == null || arenaName == null) {
            player.closeInventory();
            return;
        }

        Player target = Bukkit.getPlayer(targetUUID);
        if (target == null || !target.isOnline()) {
            player.sendMessage(Component.text("Player is no longer online.", NamedTextColor.RED));
            player.closeInventory();
            plugin.getGUIManager().clearChallengeData(player.getUniqueId());
            return;
        }

        player.closeInventory();
        plugin.getGUIManager().clearChallengeData(player.getUniqueId());

        // Send the challenge
        boolean sent = plugin.getDuelManager().sendRequest(player.getUniqueId(), targetUUID, arenaName, kit.getName());
        if (!sent) {
            player.sendMessage(Component.text("Could not send duel request. You may already have a pending request.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("Duel request sent to " + target.getName() + "!", NamedTextColor.GREEN));
        player.sendMessage(Component.text("Arena: " + arenaName + " | Kit: " + kit.getName(), NamedTextColor.GRAY));

        target.sendMessage(Component.empty());
        target.sendMessage(Component.text("=========================", NamedTextColor.GOLD));
        target.sendMessage(Component.text(player.getName(), NamedTextColor.YELLOW)
                .append(Component.text(" challenged you to a duel!", NamedTextColor.GREEN)));
        target.sendMessage(Component.text("Arena: ", NamedTextColor.GRAY)
                .append(Component.text(arenaName, NamedTextColor.WHITE))
                .append(Component.text(" | Kit: ", NamedTextColor.GRAY))
                .append(Component.text(kit.getName(), NamedTextColor.WHITE)));
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

        // Allow interaction with the inventory contents (slots 0-52)
        // But handle the save button (slot 53)
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

            // Read contents from slots 0-35
            ItemStack[] contents = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                ItemStack item = inv.getItem(i);
                contents[i] = item != null ? item.clone() : null;
            }

            // Read armor from slots 36-39
            ItemStack[] armor = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                ItemStack item = inv.getItem(36 + i);
                armor[i] = item != null ? item.clone() : null;
            }

            // Read offhand from slot 40
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
