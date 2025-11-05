package hs.elementPlugin.elements.impl.metal.listeners;

import hs.elementPlugin.elements.abilities.impl.metal.MetalChainAbility;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class MetalChainStunListener implements Listener {

    /**
     * Prevent stunned entities from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMove(PlayerMoveEvent event) {
        // Check if player is stunned
        if (event.getPlayer().hasMetadata(MetalChainAbility.META_CHAINED_STUN)) {
            long stunUntil = event.getPlayer().getMetadata(MetalChainAbility.META_CHAINED_STUN).get(0).asLong();

            // Check if stun is still active
            if (System.currentTimeMillis() < stunUntil) {
                // Cancel horizontal movement (but allow vertical for gravity)
                if (event.getFrom().getX() != event.getTo().getX() ||
                        event.getFrom().getZ() != event.getTo().getZ()) {
                    event.setCancelled(true);

                    // Set velocity to zero to stop all movement
                    event.getPlayer().setVelocity(new Vector(0, event.getPlayer().getVelocity().getY(), 0));
                }
            }
        }
    }

    /**
     * Prevent stunned entities from taking knockback
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Check if entity is stunned
        if (entity.hasMetadata(MetalChainAbility.META_CHAINED_STUN)) {
            long stunUntil = entity.getMetadata(MetalChainAbility.META_CHAINED_STUN).get(0).asLong();

            // Check if stun is still active
            if (System.currentTimeMillis() < stunUntil) {
                // Cancel velocity changes from damage
                entity.setVelocity(new Vector(0, 0, 0));
            }
        }
    }
}