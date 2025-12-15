package saturn.elementPlugin.elements.impl.death.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles Death element XP boost
 * Death Upside 1: 25% more XP from ALL entity kills (not just mobs)
 */
public class DeathXPDropListener implements Listener {
    private final ElementManager elementManager;

    public DeathXPDropListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();

        // FIXED: Check if ANY player killed this entity (not just Player killer)
        Player killer = entity.getKiller();

        // If no direct killer, check for last damager
        if (killer == null && entity.getLastDamageCause() != null) {
            if (entity.getLastDamageCause() instanceof org.bukkit.event.entity.EntityDamageByEntityEvent damageEvent) {
                if (damageEvent.getDamager() instanceof Player) {
                    killer = (Player) damageEvent.getDamager();
                } else if (damageEvent.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
                    if (projectile.getShooter() instanceof Player) {
                        killer = (Player) projectile.getShooter();
                    }
                }
            }
        }

        // If still no killer, return
        if (killer == null) {
            return;
        }

        // Check if killer has Death element
        var playerData = elementManager.data(killer.getUniqueId());
        if (playerData.getCurrentElement() != ElementType.DEATH) {
            return;
        }

        // Get current XP drop
        int originalXP = event.getDroppedExp();

        if (originalXP > 0) {
            // Increase by 25% (multiply by 1.25)
            int bonusXP = (int) Math.ceil(originalXP * 0.25);
            event.setDroppedExp(originalXP + bonusXP);
        }
    }
}