package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.entity.Player;

public class WorldProtectionListener implements Listener {

    private final EpicDuels plugin;

    public WorldProtectionListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName();

        // Always allow in arena template worlds
        if (worldName.startsWith("arena_template_")) return;

        // Block in instance worlds
        if (worldName.startsWith("arena_instance_")) {
            event.setCancelled(true);
            return;
        }

        // Block in lobby for non-admins
        if (!event.getPlayer().hasPermission("epicduels.admin")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();

        // Always allow in arena template worlds
        if (worldName.startsWith("arena_template_")) return;

        // Block in instance worlds
        if (worldName.startsWith("arena_instance_")) {
            event.setCancelled(true);
            return;
        }

        // Block in lobby for non-admins
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
