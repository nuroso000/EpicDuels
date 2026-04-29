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

import java.util.UUID;

public class PlayerListener implements Listener {

    private final EpicDuels plugin;

    public PlayerListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setHealth(player.getMaxHealth());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.setGameMode(org.bukkit.GameMode.ADVENTURE);
        player.teleport(plugin.getLobbyLocation());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Handle disconnect during duel
        plugin.getDuelManager().handleDisconnect(player.getUniqueId());
        // Remove from spectating
        plugin.getDuelManager().removeSpectator(player.getUniqueId());
        // Remove from queue
        plugin.getQueueManager().removePlayer(player.getUniqueId());
        // Cancel any pending requests
        plugin.getDuelManager().cancelRequest(player.getUniqueId());
        plugin.getDuelManager().denyRequest(player.getUniqueId());
        // Clear GUI data
        plugin.getGUIManager().clearChallengeData(player.getUniqueId());
        // Party / team duel / tournament cleanup
        if (plugin.getTeamDuelManager() != null) plugin.getTeamDuelManager().handleDisconnect(player.getUniqueId());
        if (plugin.getTournamentManager() != null) plugin.getTournamentManager().handleDisconnect(player.getUniqueId());
        if (plugin.getPartyManager() != null) plugin.getPartyManager().handleDisconnect(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        if (!plugin.getDuelManager().isFrozen(id) && !plugin.getTeamDuelManager().isFrozen(id)) return;

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

        if (plugin.getDuelManager().isFrozen(player.getUniqueId())
                || plugin.getTeamDuelManager().isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;

        if (plugin.getDuelManager().isFrozen(damager.getUniqueId())
                || plugin.getTeamDuelManager().isFrozen(damager.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // Disable PvP in lobby if configured
        if (plugin.getConfig().getBoolean("lobby.disable-pvp", true)) {
            if (event.getEntity() instanceof Player victim) {
                boolean damagerInDuel = plugin.getDuelManager().isInDuel(damager.getUniqueId())
                        || plugin.getTeamDuelManager().isInTeamDuel(damager.getUniqueId());
                boolean victimInDuel = plugin.getDuelManager().isInDuel(victim.getUniqueId())
                        || plugin.getTeamDuelManager().isInTeamDuel(victim.getUniqueId());
                if (!damagerInDuel || !victimInDuel) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();
        DuelInstance duel = plugin.getDuelManager().getDuel(deceased.getUniqueId());
        dev.epicduels.model.TeamDuelInstance teamDuel = plugin.getTeamDuelManager() != null
                ? plugin.getTeamDuelManager().getTeamDuelOf(deceased.getUniqueId()) : null;

        if ((duel == null || !duel.isActive()) && (teamDuel == null || !teamDuel.isActive())) return;

        // Suppress death message and drops
        event.deathMessage(null);
        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true);
        event.setKeepLevel(true);

        // Instant respawn so players don't get stuck on the death screen
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
            if (!deceased.isDead()) return;
            try {
                deceased.spigot().respawn();
            } catch (Throwable ignored) {
            }
        });

        if (teamDuel != null && teamDuel.isActive()) {
            plugin.getTeamDuelManager().handleDeath(deceased);
            return;
        }

        // End the 1v1 duel - the opponent wins
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
