package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import dev.epicduels.model.DuelInstance;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerListener implements Listener {

    private final EpicDuels plugin;

    public PlayerListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.teleport(plugin.getLobbyLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Handle disconnect during duel
        plugin.getDuelManager().handleDisconnect(player.getUniqueId());
        // Cancel any pending requests
        plugin.getDuelManager().cancelRequest(player.getUniqueId());
        plugin.getDuelManager().denyRequest(player.getUniqueId());
        // Clear GUI data
        plugin.getGUIManager().clearChallengeData(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getDuelManager().isFrozen(event.getPlayer().getUniqueId())) return;

        // Allow head rotation but not movement
        if (event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (plugin.getDuelManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        if (plugin.getDuelManager().isFrozen(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        DuelInstance duel = plugin.getDuelManager().getDuel(deceased.getUniqueId());
        if (duel == null || !duel.isActive()) return;

        // Suppress death message and drops in duels
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        // End the duel - the opponent wins
        java.util.UUID winnerId = duel.getOpponent(deceased.getUniqueId());
        plugin.getDuelManager().endDuel(duel, winnerId, deceased.getUniqueId());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // If player died in duel, respawn at lobby
        Player player = event.getPlayer();
        event.setRespawnLocation(plugin.getLobbyLocation());
    }
}
