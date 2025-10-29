package hs.elementPlugin.elements.impl.fire.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.TrustManager;
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
    private final TrustManager trustManager;

    public FireCombatListener(ElementManager elementManager, TrustManager trustManager) {
        this.elementManager = elementManager;
        this.trustManager = trustManager;
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

        // Don't apply to trusted players
        if (event.getEntity() instanceof Player victim) {
            if (trustManager.isTrusted(damager.getUniqueId(), victim.getUniqueId())) {
                return;
            }
        }

        // Apply fire aspect (set entity on fire for 4 seconds)
        event.getEntity().setFireTicks(80); // 80 ticks = 4 seconds
    }
}