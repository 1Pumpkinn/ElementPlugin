package hs.elementPlugin.elements.air;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class AirListener implements Listener {
    private final ElementManager elementManager;

    public AirListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }
    
    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Check if player has Air element
        if (elementManager.getPlayerElement(player) != ElementType.AIR) return;
        
        // Reduce fall damage for Air element users
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(event.getDamage() * 0.5); // 50% fall damage reduction
        }
    }
}