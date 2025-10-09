package hs.elementPlugin.elements.impl.water.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class WaterDrowningImmunityListener implements Listener {
    private final ElementManager elementManager;

    public WaterDrowningImmunityListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onWaterDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (elementManager.getPlayerElement(player) != ElementType.WATER) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.DROWNING) {
            event.setCancelled(true);
        }
    }
}
