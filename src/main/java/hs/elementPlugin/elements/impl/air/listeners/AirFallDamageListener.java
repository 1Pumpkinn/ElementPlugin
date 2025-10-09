package hs.elementPlugin.elements.impl.air.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class AirFallDamageListener implements Listener {
    private final ElementManager elementManager;

    public AirFallDamageListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (elementManager.getPlayerElement(player) != ElementType.AIR) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setDamage(event.getDamage() * 0.5); // 50% fall damage reduction
        }
    }
}
