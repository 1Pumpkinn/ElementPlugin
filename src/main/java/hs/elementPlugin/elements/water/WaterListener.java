package hs.elementPlugin.elements.water;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class WaterListener implements Listener {
    private final ElementManager elementManager;

    public WaterListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }
    
    @EventHandler
    public void onWaterDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Check if player has Water element
        if (elementManager.getPlayerElement(player) != ElementType.WATER) return;
        
        // Cancel drowning damage for Water element users
        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            event.setCancelled(true);
        }
    }
}