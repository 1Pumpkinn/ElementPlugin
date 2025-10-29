package hs.elementPlugin.elements.impl.fire.listeners;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Prevents Fire element players from being damaged by their own fireballs
 */
public class FireballProtectionListener implements Listener {

    @EventHandler
    public void onFireballDamage(EntityDamageByEntityEvent event) {
        // Check if victim is a player
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        // Check if damager is a fireball
        if (!(event.getDamager() instanceof Fireball fireball)) {
            return;
        }

        // Check if the fireball was shot by the victim
        if (fireball.getShooter() instanceof Player shooter) {
            if (shooter.getUniqueId().equals(victim.getUniqueId())) {
                // Cancel self-damage from own fireball
                event.setCancelled(true);
            }
        }
    }
}