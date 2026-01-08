package saturn.elementPlugin.elements.impl.metal.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

/**
 * Metal element passive: Deflect arrows upward when hit
 * FIXED: Removed deprecated setBounce() call
 */
public class MetalDeflectArrows implements Listener {

    private final ElementManager elementManager;

    public MetalDeflectArrows(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowHit(ProjectileHitEvent event) {
        // Check if projectile is an arrow
        if (!(event.getEntity() instanceof Arrow arrow)) {
            return;
        }

        // Check if it hit a player
        if (!(event.getHitEntity() instanceof Player hitPlayer)) {
            return;
        }

        // Check if player has Metal element
        var playerData = elementManager.data(hitPlayer.getUniqueId());
        if (playerData.getCurrentElement() != ElementType.METAL) {
            return;
        }

        // Cancel the arrow damage
        event.setCancelled(true);

        // Get current velocity and deflect upward with 2x force
        Vector velocity = arrow.getVelocity();

        // Reverse horizontal direction and double upward force
        velocity.setX(-velocity.getX());
        velocity.setZ(-velocity.getZ());
        velocity.setY(Math.abs(velocity.getY()) * 2);

        // Apply new velocity
        arrow.setVelocity(velocity);

        // REMOVED: arrow.setBounce(false); - This method is deprecated
        // Note: The arrow will still deflect correctly without this call

        // Play metallic deflect sound
        hitPlayer.getWorld().playSound(
                hitPlayer.getLocation(),
                Sound.BLOCK_ANVIL_LAND,
                0.5f,
                2.0f
        );

        // Visual particle effect
        hitPlayer.getWorld().spawnParticle(
                org.bukkit.Particle.CRIT,
                hitPlayer.getLocation().add(0, 1, 0),
                15, 0.3, 0.5, 0.3, 0.1
        );
    }
}