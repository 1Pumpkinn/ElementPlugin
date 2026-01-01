package saturn.elementPlugin.elements.impl.air.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Handles Air element combat interactions
 * Air Upside 2: Hitting someone has 5% chance to give them slow falling (Upgrade II)
 */
public class AirCombatListener implements Listener {
    private final ElementManager elementManager;

    public AirCombatListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if damager is an Air element player
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        // Check if target is a living entity
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        var playerData = elementManager.data(damager.getUniqueId());
        if (playerData.getCurrentElement() != ElementType.AIR) {
            return;
        }

        // Check if they have Upgrade 2
        if (playerData.getUpgradeLevel(ElementType.AIR) < 2) {
            return;
        }

        // 5% chance to apply slow falling
        if (Math.random() < 0.05) {
            // Apply Slow Falling for 5 seconds
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOW_FALLING,
                    100, // 5 seconds
                    0,
                    false,
                    true,
                    true
            ));

            // Visual feedback
            target.getWorld().spawnParticle(
                    org.bukkit.Particle.CLOUD,
                    target.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0.05
            );
        }
    }
}