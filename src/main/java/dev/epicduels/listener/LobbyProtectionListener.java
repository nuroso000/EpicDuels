package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Applies lobby-world-only protections. The lobby world is any world that
 * is not an arena template or instance world. Admins keep their existing
 * bypass for block interaction so they can still set up the lobby.
 */
public class LobbyProtectionListener implements Listener {

    private final EpicDuels plugin;

    public LobbyProtectionListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    private boolean isLobby(World world) {
        if (world == null) return false;
        String name = world.getName();
        return !name.startsWith("arena_template_") && !name.startsWith("arena_instance_");
    }

    // --- Disable block interaction ---
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!isLobby(event.getClickedBlock().getWorld())) return;
        if (event.getPlayer().hasPermission("epicduels.admin")) return;
        event.setCancelled(true);
    }

    // --- Disable fall damage & void death (teleport to spawn) ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobby(player.getWorld())) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.VOID) {
            event.setCancelled(true);
            if (plugin.getLobbyLocation() != null) {
                player.teleport(plugin.getLobbyLocation());
            }
        }
    }

    // --- Disable weather change in the lobby world ---
    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        if (!isLobby(event.getWorld())) return;
        if (event.toWeatherState()) event.setCancelled(true);
    }

    // --- Disable item pickup ---
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobby(player.getWorld())) return;
        event.setCancelled(true);
    }

    // --- Disable item drop ---
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!isLobby(event.getPlayer().getWorld())) return;
        event.setCancelled(true);
    }

    // --- Disable mob spawning ---
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!isLobby(event.getLocation().getWorld())) return;
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        // Allow admin/command-spawned entities
        if (reason == CreatureSpawnEvent.SpawnReason.COMMAND
                || reason == CreatureSpawnEvent.SpawnReason.CUSTOM
                || reason == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
            return;
        }
        event.setCancelled(true);
    }

    // --- Disable fire spread ---
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (!isLobby(event.getBlock().getWorld())) return;
        if (event.getNewState().getType().name().contains("FIRE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!isLobby(event.getBlock().getWorld())) return;
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD
                || event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
            event.setCancelled(true);
        }
    }

    // --- Disable block burning ---
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!isLobby(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }

    // --- Disable death messages in lobby ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isLobby(event.getEntity().getWorld())) return;
        event.deathMessage(null);
    }

    // --- Disable leaf decay ---
    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!isLobby(event.getBlock().getWorld())) return;
        event.setCancelled(true);
    }
}
