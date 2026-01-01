package saturn.elementPlugin.elements.impl.frost.listeners;

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
 * Handles Frost element combat interactions
 * Frost Upside 2: 10% chance to apply freeze effect when hitting enemies (Upgrade II)
 */
public class FrostCombatListener implements Listener {
    private final ElementManager elementManager;

    public FrostCombatListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if damager is a Frost element player
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        // Check if target is a living entity
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        var playerData = elementManager.data(damager.getUniqueId());
        if (playerData.getCurrentElement() != ElementType.FROST) {
            return;
        }

        // Check if they have Upgrade 2
        if (playerData.getUpgradeLevel(ElementType.FROST) < 2) {
            return;
        }

        // UPDATED: 10% chance to apply freeze effect
        if (Math.random() < 0.10) {
            // Apply Slowness 4 for 3 seconds (freeze effect)
            target.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    60, // 3 seconds (60 ticks)
                    3, // Slowness 4
                    false,
                    true,
                    true
            ));

            // Apply visual freeze ticks (makes entity look frozen)
            target.setFreezeTicks(target.getMaxFreezeTicks());

            // Visual particles
            target.getWorld().spawnParticle(
                    org.bukkit.Particle.SNOWFLAKE,
                    target.getLocation().add(0, 1, 0),
                    20, 0.3, 0.5, 0.3, 0.05
            );
        }
    }
}