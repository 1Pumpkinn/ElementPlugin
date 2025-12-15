package saturn.elementPlugin.elements.impl.earth.listeners;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.abilities.impl.earth.EarthquakeAbility;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

/**
 * Handles movement prevention for entities stunned by Earthquake
 */
public class EarthquakeMovementListener implements Listener {

    private final ElementPlugin plugin;

    public EarthquakeMovementListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent stunned players from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player is stunned by Earthquake
        if (!player.hasMetadata(EarthquakeAbility.META_EARTHQUAKE_STUNNED)) {
            return;
        }

        long stunUntil = player.getMetadata(EarthquakeAbility.META_EARTHQUAKE_STUNNED).get(0).asLong();

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
            player.removeMetadata(EarthquakeAbility.META_EARTHQUAKE_STUNNED, plugin);
        }
    }

    /**
     * Prevent stunned mobs from moving
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();

        // Skip players (handled by PlayerMoveEvent)
        if (entity instanceof Player) {
            return;
        }

        // Check if entity is stunned by Earthquake
        if (!entity.hasMetadata(EarthquakeAbility.META_EARTHQUAKE_STUNNED)) {
            return;
        }

        long stunUntil = entity.getMetadata(EarthquakeAbility.META_EARTHQUAKE_STUNNED).get(0).asLong();

        // Check if stun is still active
        if (System.currentTimeMillis() < stunUntil) {
            // Cancel the movement
            event.setCancelled(true);

            // Set velocity to zero
            entity.setVelocity(new Vector(0, 0, 0));
        } else {
            // Stun expired, remove metadata
            entity.removeMetadata(EarthquakeAbility.META_EARTHQUAKE_STUNNED, plugin);
        }
    }
}