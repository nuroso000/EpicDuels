package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.DuelInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public class WorldProtectionListener implements Listener {

    private final EpicDuels plugin;

    public WorldProtectionListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();

        // Always allow in arena template worlds (admin building)
        if (worldName.startsWith("arena_template_")) return;

        if (worldName.startsWith("arena_instance_")) {
            DuelInstance duel = plugin.getDuelManager().getDuelByWorld(worldName);
            if (duel == null || !duel.isActive()) {
                event.setCancelled(true);
                return;
            }
            // Record this as a player-placed block so it can be broken later
            Block b = event.getBlock();
            duel.recordPlayerBlock(b.getX(), b.getY(), b.getZ());
            return;
        }

        // Protect lobby for non-admins
        if (!event.getPlayer().hasPermission("epicduels.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName();

        // Always allow in arena template worlds (admin building)
        if (worldName.startsWith("arena_template_")) return;

        if (worldName.startsWith("arena_instance_")) {
            DuelInstance duel = plugin.getDuelManager().getDuelByWorld(worldName);
            if (duel == null || !duel.isActive()) {
                event.setCancelled(true);
                return;
            }

            Block b = event.getBlock();
            if (duel.isPlayerPlacedBlock(b.getX(), b.getY(), b.getZ())) {
                // Player-placed block — allow and remove from tracking
                duel.removePlayerBlock(b.getX(), b.getY(), b.getZ());
            } else {
                // Original map block — deny
                event.setCancelled(true);
            }
            return;
        }

        // Protect lobby for non-admins
        if (!event.getPlayer().hasPermission("epicduels.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        String worldName = player.getWorld().getName();

        // Prevent hunger in lobby
        if (!worldName.startsWith("arena_instance_") && !worldName.startsWith("arena_template_")) {
            event.setCancelled(true);
        }
    }
}
