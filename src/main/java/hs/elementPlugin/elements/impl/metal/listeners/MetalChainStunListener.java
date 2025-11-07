package hs.elementPlugin.elements.impl.metal.listeners;

import hs.elementPlugin.elements.abilities.impl.metal.MetalChainAbility;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

public class MetalChainStunListener implements Listener {

    /**
     * Prevent stunned players from moving or jumping
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player is stunned
        if (player.hasMetadata(MetalChainAbility.META_CHAINED_STUN)) {
            long stunUntil = player.getMetadata(MetalChainAbility.META_CHAINED_STUN).get(0).asLong();

            // Check if stun is still active
            if (System.currentTimeMillis() < stunUntil) {
                // Cancel ALL movement (horizontal and vertical)
                if (event.getFrom().getX() != event.getTo().getX() ||
                        event.getFrom().getY() != event.getTo().getY() ||
                        event.getFrom().getZ() != event.getTo().getZ()) {
                    event.setCancelled(true);

                    // Set velocity to zero to prevent all movement including jumping
                    player.setVelocity(new Vector(0, 0, 0));
                }
            } else {
                // Stun expired, remove metadata
                player.removeMetadata(MetalChainAbility.META_CHAINED_STUN,
                        player.getServer().getPluginManager().getPlugin("ElementPlugin"));
            }
        }
    }

    /**
     * Prevent stunned mobs from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();

        // Skip players (handled by PlayerMoveEvent)
        if (entity instanceof Player) return;

        // Check if entity is stunned
        if (entity.hasMetadata(MetalChainAbility.META_CHAINED_STUN)) {
            long stunUntil = entity.getMetadata(MetalChainAbility.META_CHAINED_STUN).get(0).asLong();

            // Check if stun is still active
            if (System.currentTimeMillis() < stunUntil) {
                // Cancel the movement
                event.setCancelled(true);

                // Set velocity to zero
                entity.setVelocity(new Vector(0, 0, 0));
            } else {
                // Stun expired, remove metadata
                entity.removeMetadata(MetalChainAbility.META_CHAINED_STUN,
                        entity.getServer().getPluginManager().getPlugin("ElementPlugin"));
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
            } else {
                // Stun expired, remove metadata
                entity.removeMetadata(MetalChainAbility.META_CHAINED_STUN,
                        entity.getServer().getPluginManager().getPlugin("ElementPlugin"));
            }
        }
    }
}