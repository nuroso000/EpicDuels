package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * Applies lobby-world-only protections. The lobby world is any world that
 * is not an arena template or instance world. Every handler is gated by
 * a toggle under {@code lobby.protections.*} in config.yml so server
 * admins can turn individual rules on or off.
 */
public class LobbyProtectionListener implements Listener {

    private final EpicDuels plugin;
    private final Set<UUID> bypassing = new HashSet<>();

    public LobbyProtectionListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    public void setBypass(UUID player, boolean bypass) {
        if (bypass) {
            bypassing.add(player);
        } else {
            bypassing.remove(player);
        }
    }

    public boolean isBypassing(UUID player) {
        return bypassing.contains(player);
    }

    private boolean isLobby(World world) {
        if (world == null) return false;
        String name = world.getName();
        return !name.startsWith("arena_template_") && !name.startsWith("arena_instance_");
    }

    private boolean enabled(String key) {
        return plugin.getConfig().getBoolean("lobby.protections." + key, true);
    }

    // --- Disable block interaction ---
    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        if (!isLobby(event.getClickedBlock().getWorld())) return;
        if (!enabled("block-interact")) return;
        if (bypassing.contains(event.getPlayer().getUniqueId())) return;
        if (event.getPlayer().hasPermission("epicduels.admin")) return;
        event.setCancelled(true);
    }

    // --- Disable entity interaction (item frames, armor stands, etc.) ---
    @EventHandler(priority = EventPriority.LOW)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!isLobby(event.getPlayer().getWorld())) return;
        if (!enabled("block-interact")) return;
        if (bypassing.contains(event.getPlayer().getUniqueId())) return;
        if (event.getPlayer().hasPermission("epicduels.admin")) return;
        event.setCancelled(true);
    }

    // --- Disable fall damage & void death (teleport to spawn) ---
    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobby(player.getWorld())) return;
        if (bypassing.contains(player.getUniqueId())) return;

        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FALL && enabled("fall-damage")) {
            event.setCancelled(true);
            return;
        }
        if (cause == EntityDamageEvent.DamageCause.VOID && enabled("void-death")) {
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
        if (!enabled("weather")) return;
        if (event.toWeatherState()) event.setCancelled(true);
    }

    // --- Disable item pickup ---
    @EventHandler
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isLobby(player.getWorld())) return;
        if (!enabled("item-pickup")) return;
        if (bypassing.contains(player.getUniqueId())) return;
        event.setCancelled(true);
    }

    // --- Disable item drop ---
    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (!isLobby(event.getPlayer().getWorld())) return;
        if (!enabled("item-drop")) return;
        if (bypassing.contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
    }

    // --- Disable mob spawning ---
    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (!isLobby(event.getLocation().getWorld())) return;
        if (!enabled("mob-spawning")) return;
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
        if (!enabled("fire-spread")) return;
        if (event.getNewState().getType().name().contains("FIRE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!isLobby(event.getBlock().getWorld())) return;
        if (!enabled("fire-spread")) return;
        if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD
                || event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
            event.setCancelled(true);
        }
    }

    // --- Disable block burning ---
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (!isLobby(event.getBlock().getWorld())) return;
        if (!enabled("block-burning")) return;
        event.setCancelled(true);
    }

    // --- Disable death messages in lobby ---
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isLobby(event.getEntity().getWorld())) return;
        if (!enabled("death-messages")) return;
        event.deathMessage(null);
    }

    // --- Disable leaf decay ---
    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (!isLobby(event.getBlock().getWorld())) return;
        if (!enabled("leaf-decay")) return;
        event.setCancelled(true);
    }

    // --- Disable inventory movement (but allow plugin GUIs) ---
    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isLobby(player.getWorld())) return;
        if (!enabled("inventory-movement")) return;
        if (bypassing.contains(player.getUniqueId())) return;
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isLobby(player.getWorld())) return;
        if (!enabled("inventory-movement")) return;
        if (bypassing.contains(player.getUniqueId())) return;
        if (event.getView().getTopInventory().getType() != InventoryType.CRAFTING) return;
        event.setCancelled(true);
    }
}
