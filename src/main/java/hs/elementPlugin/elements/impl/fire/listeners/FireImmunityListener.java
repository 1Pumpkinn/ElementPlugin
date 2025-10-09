package hs.elementPlugin.elements.impl.fire.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class FireImmunityListener implements Listener {
    private final ElementManager elementManager;

    public FireImmunityListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onFireDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (elementManager.getPlayerElement(player) == ElementType.FIRE) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FIRE ||
                event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK ||
                event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }
}
