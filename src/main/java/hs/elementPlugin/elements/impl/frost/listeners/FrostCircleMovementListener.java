package hs.elementPlugin.elements.impl.frost.listeners;

import hs.elementPlugin.elements.abilities.impl.frost.FrostCircleAbility;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/**
 * Prevents movement and jumping for entities frozen by Frost Circle
 */
public class FrostCircleMovementListener implements Listener {

    /**
     * Prevent frozen players from moving or jumping
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player is frozen by circle
        if (!player.hasMetadata(FrostCircleAbility.META_CIRCLE_FROZEN)) {
            return;
        }

        // Cancel ALL movement (horizontal and vertical)
        if (event.getFrom().getX() != event.getTo().getX() ||
                event.getFrom().getY() != event.getTo().getY() ||
                event.getFrom().getZ() != event.getTo().getZ()) {
            event.setCancelled(true);

            // Set velocity to zero to prevent jumping
            player.setVelocity(new Vector(0, 0, 0));
        }
    }

    /**
     * Prevent frozen mobs from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();

        // Skip players (handled by PlayerMoveEvent)
        if (entity instanceof Player) return;

        // Check if entity is frozen by circle
        if (!entity.hasMetadata(FrostCircleAbility.META_CIRCLE_FROZEN)) {
            return;
        }

        // Cancel the movement
        event.setCancelled(true);

        // Set velocity to zero
        entity.setVelocity(new Vector(0, 0, 0));
    }
}