package hs.elementPlugin.elements.impl.metal.listeners;

import hs.elementPlugin.elements.abilities.impl.metal.MetalChainAbility;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
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
                // CRITICAL FIX: Allow falling but prevent horizontal movement
                Location from = event.getFrom();
                Location to = event.getTo();

                if (to != null) {
                    // Only cancel horizontal movement (X and Z), allow vertical (Y) for falling
                    if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                        // Player tried to move horizontally - cancel it
                        Location newTo = to.clone();
                        newTo.setX(from.getX());
                        newTo.setZ(from.getZ());
                        event.setTo(newTo);
                    }

                    // Cancel upward movement (jumping), but allow falling
                    if (to.getY() > from.getY()) {
                        Location newTo = to.clone();
                        newTo.setY(from.getY());
                        event.setTo(newTo);
                    }
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
                // CRITICAL FIX: Allow falling but prevent horizontal movement
                Location from = event.getFrom();
                Location to = event.getTo();

                // Only cancel horizontal movement (X and Z), allow vertical (Y) for falling
                if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
                    // Entity tried to move horizontally - cancel it
                    event.setCancelled(true);
                }

                // Allow falling but prevent jumping (only if moving upward)
                if (to.getY() > from.getY()) {
                    event.setCancelled(true);
                }
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
                // Cancel horizontal velocity changes from damage, but allow falling
                Vector currentVelocity = entity.getVelocity();
                // Only preserve downward Y velocity (falling), zero out X and Z
                entity.setVelocity(new Vector(0, Math.min(currentVelocity.getY(), 0), 0));
            } else {
                // Stun expired, remove metadata
                entity.removeMetadata(MetalChainAbility.META_CHAINED_STUN,
                        entity.getServer().getPluginManager().getPlugin("ElementPlugin"));
            }
        }
    }
}