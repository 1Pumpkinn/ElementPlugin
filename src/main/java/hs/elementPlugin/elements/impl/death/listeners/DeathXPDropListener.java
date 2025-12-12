package hs.elementPlugin.elements.impl.death.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Handles Death element XP boost
 * Death Upside 1: 25% more XP from mob kills
 */
public class DeathXPDropListener implements Listener {
    private final ElementManager elementManager;

    public DeathXPDropListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        // Check if killer is a player
        if (!(event.getEntity().getKiller() instanceof Player killer)) {
            return;
        }

        // Check if killer has Death element
        var playerData = elementManager.data(killer.getUniqueId());
        if (playerData.getCurrentElement() != ElementType.DEATH) {
            return;
        }

        // Get current XP drop
        int originalXP = event.getDroppedExp();

        if (originalXP > 0) {
            // Increase by 25% (multiply by 1.25)
            int bonusXP = (int) Math.ceil(originalXP * 0.25);
            event.setDroppedExp(originalXP + bonusXP);
        }
        }
    }
