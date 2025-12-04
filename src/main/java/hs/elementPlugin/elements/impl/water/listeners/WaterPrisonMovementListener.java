package hs.elementPlugin.elements.impl.water.listeners;

import hs.elementPlugin.elements.abilities.impl.water.WaterPrisonAbility;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/**
 * Prevents movement for entities trapped in Water Prison
 */
public class WaterPrisonMovementListener implements Listener {

    /**
     * Prevent players trapped in Water Prison from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player is trapped in water prison
        if (player.hasMetadata(WaterPrisonAbility.META_WATER_PRISON)) {
            long trapUntil = player.getMetadata(WaterPrisonAbility.META_WATER_PRISON).get(0).asLong();

            // Check if trap is still active
            if (System.currentTimeMillis() < trapUntil) {
                // Cancel ALL movement (horizontal and vertical)
                if (event.getFrom().getX() != event.getTo().getX() ||
                        event.getFrom().getY() != event.getTo().getY() ||
                        event.getFrom().getZ() != event.getTo().getZ()) {
                    event.setCancelled(true);

                    // Set velocity to zero to prevent all movement
                    player.setVelocity(new Vector(0, 0, 0));
                }
            } else {
                // Trap expired, remove metadata
                player.removeMetadata(WaterPrisonAbility.META_WATER_PRISON,
                        player.getServer().getPluginManager().getPlugin("ElementPlugin"));
            }
        }
    }

    /**
     * Prevent mobs trapped in Water Prison from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();

        // Skip players (handled by PlayerMoveEvent)
        if (entity instanceof Player) return;

        // Check if entity is trapped in water prison
        if (entity.hasMetadata(WaterPrisonAbility.META_WATER_PRISON)) {
            long trapUntil = entity.getMetadata(WaterPrisonAbility.META_WATER_PRISON).get(0).asLong();

            // Check if trap is still active
            if (System.currentTimeMillis() < trapUntil) {
                // Cancel the movement
                event.setCancelled(true);

                // Set velocity to zero
                entity.setVelocity(new Vector(0, 0, 0));
            } else {
                // Trap expired, remove metadata
                entity.removeMetadata(WaterPrisonAbility.META_WATER_PRISON,
                        entity.getServer().getPluginManager().getPlugin("ElementPlugin"));
            }
        }
    }
}