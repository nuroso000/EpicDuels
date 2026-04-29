package dev.epicduels.listener;

import dev.epicduels.EpicDuels;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class FriendlyFireListener implements Listener {

    private final EpicDuels plugin;

    public FriendlyFireListener(EpicDuels plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (event.getDamager() instanceof Player p) {
            attacker = p;
        } else if (event.getDamager() instanceof Projectile proj
                && proj.getShooter() instanceof Player shooter) {
            attacker = shooter;
        }
        if (attacker == null) return;
        if (attacker.equals(victim)) return;

        if (plugin.getTeamDuelManager().sameTeam(attacker.getUniqueId(), victim.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
