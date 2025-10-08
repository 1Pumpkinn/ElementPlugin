package hs.elementPlugin.elements.life;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class LifeListener implements Listener {
    private final ElementManager elementManager;

    public LifeListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }
    
    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        
        // Check if player has Life element
        if (elementManager.getPlayerElement(player) != ElementType.LIFE) return;
        
        // Boost natural regeneration for Life element users
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
            event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setAmount(event.getAmount() * 1.5); // 50% faster natural regeneration
        }
    }
}