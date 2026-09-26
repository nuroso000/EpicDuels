package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import dev.epicduels.gui.MenuHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.inventory.InventoryType;

/** Prevents snapshot items from escaping into persistent plugin or world storage. */
public final class InventoryGuardListener implements Listener {
    private final EpicDuels plugin;
    public InventoryGuardListener(EpicDuels plugin) { this.plugin = plugin; }

    private boolean protectedPlayer(Player player) {
        return plugin.getInventoryBackupManager().hasBackup(player.getUniqueId());
    }

    private boolean external(org.bukkit.inventory.Inventory inventory) {
        return !(inventory.getHolder(false) instanceof MenuHolder)
                && inventory.getType() != InventoryType.CRAFTING
                && inventory.getType() != InventoryType.PLAYER;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player && protectedPlayer(player) && external(event.getInventory()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && protectedPlayer(player) && external(event.getInventory()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && protectedPlayer(player) && external(event.getInventory()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(org.bukkit.event.player.PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!protectedPlayer(player)) return;
        dev.epicduels.model.DuelInstance duel = plugin.getDuelManager().getDuel(player.getUniqueId());
        dev.epicduels.model.TeamDuelInstance team = plugin.getTeamDuelManager().getTeamDuelOf(player.getUniqueId());
        boolean fighting = duel != null && duel.isActive() && player.getWorld().equals(duel.getInstanceWorld())
                || team != null && team.isActive() && team.isAlive(player.getUniqueId()) && player.getWorld().equals(team.getInstanceWorld());
        if (!fighting) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!protectedPlayer(event.getPlayer())) return;
        String[] words = event.getMessage().trim().toLowerCase(java.util.Locale.ROOT).split("\\s+");
        String command = words[0];
        // Only the plugin's own surrender command can run while an inventory snapshot is held.
        if (!(command.equals("/duel") || command.equals("/epicduels:duel") || command.equals("/d") || command.equals("/epicduels:d"))
                || words.length != 2 || !words[1].equals("forfeit")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("Commands are unavailable until your duel inventory has been restored.");
        }
    }
}
