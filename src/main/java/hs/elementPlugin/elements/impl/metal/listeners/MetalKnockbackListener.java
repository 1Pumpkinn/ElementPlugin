package hs.elementPlugin.elements.impl.metal.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

/**
 * Handles Metal element knockback reduction
 * Metal Upside 2: Take 50% less knockback (requires Upgrade II)
 */
public class MetalKnockbackListener implements Listener {
    private final ElementManager elementManager;

    public MetalKnockbackListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if victim is a Metal element player
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        var playerData = elementManager.data(victim.getUniqueId());
        if (playerData.getCurrentElement() != ElementType.METAL) {
            return;
        }

        // Check if they have Upgrade 2
        if (playerData.getUpgradeLevel(ElementType.METAL) < 2) {
            return;
        }

        // Schedule knockback reduction for next tick (after vanilla knockback is applied)
        org.bukkit.Bukkit.getScheduler().runTask(
                elementManager.getPlugin(),
                () -> {
                    if (victim.isValid() && !victim.isDead()) {
                        Vector velocity = victim.getVelocity();
                        // Reduce horizontal knockback by 50%
                        velocity.setX(velocity.getX() * 0.5);
                        velocity.setZ(velocity.getZ() * 0.5);
                        // Keep vertical knockback (Y) unchanged for jump mechanics
                        victim.setVelocity(velocity);
                    }
                }
        );
    }
}