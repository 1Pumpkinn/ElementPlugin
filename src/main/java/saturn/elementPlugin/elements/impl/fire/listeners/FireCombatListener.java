package saturn.elementPlugin.elements.impl.fire.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Handles Fire element combat interactions
 * Fire Upside 2: Apply fire aspect when hitting enemies
 */
public class FireCombatListener implements Listener {
    private final ElementManager elementManager;

    public FireCombatListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if damager is a Fire element player
        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }

        var playerData = elementManager.data(damager.getUniqueId());
        if (playerData.getCurrentElement() != ElementType.FIRE) {
            return;
        }

        // Check if they have Upgrade 2
        if (playerData.getUpgradeLevel(ElementType.FIRE) < 2) {
            return;
        }

        // Apply fire aspect (set entity on fire for 4 seconds)
        event.getEntity().setFireTicks(80); // 80 ticks = 4 seconds
    }
}