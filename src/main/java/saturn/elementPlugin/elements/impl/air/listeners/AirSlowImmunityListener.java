package saturn.elementPlugin.elements.impl.air.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Air Upside 2: Immunity to slow-inducing blocks
 * Prevents slowdown from powdered snow, soul sand, honey blocks, etc.
 */
public class AirSlowImmunityListener implements Listener {
    private final ElementManager elementManager;

    // Blocks that normally slow players
    private static final Set<Material> SLOW_BLOCKS = EnumSet.of(
            Material.POWDER_SNOW,
            Material.SOUL_SAND,
            Material.HONEY_BLOCK,
            Material.SLIME_BLOCK,
            Material.SOUL_SOIL
    );

    public AirSlowImmunityListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Check if player has Air element
        var pd = elementManager.data(player.getUniqueId());
        if (pd == null || pd.getCurrentElement() != ElementType.AIR) {
            return;
        }

        // Check blocks at player's location and below
        Material blockAt = player.getLocation().getBlock().getType();
        Material blockBelow = player.getLocation().subtract(0, 1, 0).getBlock().getType();

        // If standing on/in a slow block, remove the slowdown effect
        if (SLOW_BLOCKS.contains(blockAt) || SLOW_BLOCKS.contains(blockBelow)) {
            // Reset walk speed to normal if it's been reduced
            if (player.getWalkSpeed() < 0.2f) {
                player.setWalkSpeed(0.2f); // Default walk speed
            }
        }
    }
}