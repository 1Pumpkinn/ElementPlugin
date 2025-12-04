package hs.elementPlugin.elements.impl.frost.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.impl.frost.FrostNovaAbility;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/**
 * Handles movement prevention for entities frozen by Frost Nova
 */
public class FrostNovaMovementListener implements Listener {

    private final ElementPlugin plugin;

    public FrostNovaMovementListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent frozen players from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player is frozen by Frost Nova
        if (!player.hasMetadata(FrostNovaAbility.META_NOVA_FROZEN)) {
            return;
        }

        long freezeUntil = player.getMetadata(FrostNovaAbility.META_NOVA_FROZEN).get(0).asLong();

        // Check if freeze is still active
        if (System.currentTimeMillis() < freezeUntil) {
            // Cancel ALL movement (horizontal and vertical)
            if (event.getFrom().getX() != event.getTo().getX() ||
                    event.getFrom().getY() != event.getTo().getY() ||
                    event.getFrom().getZ() != event.getTo().getZ()) {
                event.setCancelled(true);

                // Set velocity to zero to prevent all movement including jumping
                player.setVelocity(new Vector(0, 0, 0));
            }
        } else {
            // Freeze expired, remove metadata
            player.removeMetadata(FrostNovaAbility.META_NOVA_FROZEN, plugin);
        }
    }

    /**
     * Prevent frozen mobs from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();

        // Skip players (handled by PlayerMoveEvent)
        if (entity instanceof Player) {
            return;
        }

        // Check if entity is frozen by Frost Nova
        if (!entity.hasMetadata(FrostNovaAbility.META_NOVA_FROZEN)) {
            return;
        }

        long freezeUntil = entity.getMetadata(FrostNovaAbility.META_NOVA_FROZEN).get(0).asLong();

        // Check if freeze is still active
        if (System.currentTimeMillis() < freezeUntil) {
            // Cancel the movement
            event.setCancelled(true);

            // Set velocity to zero
            entity.setVelocity(new Vector(0, 0, 0));
        } else {
            // Freeze expired, remove metadata
            entity.removeMetadata(FrostNovaAbility.META_NOVA_FROZEN, plugin);
        }
    }
}