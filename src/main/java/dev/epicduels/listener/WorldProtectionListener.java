package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.BattleInstance;
import dev.epicduels.model.DuelInstance;
import dev.epicduels.model.TeamDuelInstance;
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
            BattleInstance battle = lookupBattle(worldName);
            if (battle == null || !battle.isActive()) {
                event.setCancelled(true);
                return;
            }
            Block b = event.getBlock();
            battle.recordPlayerBlock(b.getX(), b.getY(), b.getZ());
            return;
        }

        // Protect lobby for non-admins (when enabled in config)
        if (!plugin.getConfig().getBoolean("lobby.protections.block-place", true)) return;
        if (plugin.getLobbyProtectionListener() != null
                && plugin.getLobbyProtectionListener().isBypassing(event.getPlayer().getUniqueId())) return;
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
            BattleInstance battle = lookupBattle(worldName);
            if (battle == null || !battle.isActive()) {
                event.setCancelled(true);
                return;
            }

            Block b = event.getBlock();
            if (battle.isPlayerPlacedBlock(b.getX(), b.getY(), b.getZ())) {
                battle.removePlayerBlock(b.getX(), b.getY(), b.getZ());
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // Protect lobby for non-admins (when enabled in config)
        if (!plugin.getConfig().getBoolean("lobby.protections.block-break", true)) return;
        if (plugin.getLobbyProtectionListener() != null
                && plugin.getLobbyProtectionListener().isBypassing(event.getPlayer().getUniqueId())) return;
        if (!event.getPlayer().hasPermission("epicduels.admin")) {
            event.setCancelled(true);
        }
    }

    private BattleInstance lookupBattle(String worldName) {
        DuelInstance d = plugin.getDuelManager().getDuelByWorld(worldName);
        if (d != null) return d;
        if (plugin.getTeamDuelManager() != null) {
            TeamDuelInstance t = plugin.getTeamDuelManager().getByWorld(worldName);
            if (t != null) return t;
        }
        return null;
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        String worldName = player.getWorld().getName();

        if (!worldName.startsWith("arena_instance_") && !worldName.startsWith("arena_template_")) {
            if (plugin.getConfig().getBoolean("lobby.protections.hunger", true)) {
                if (plugin.getLobbyProtectionListener() != null
                        && plugin.getLobbyProtectionListener().isBypassing(player.getUniqueId())) return;
                event.setCancelled(true);
            }
        }
    }
}
