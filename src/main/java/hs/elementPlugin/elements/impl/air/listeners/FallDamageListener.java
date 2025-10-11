package hs.elementPlugin.elements.impl.air.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class FallDamageListener implements Listener {
    private final ElementManager elementManager;

    public FallDamageListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        // Check if damage is from falling
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;

        // Check if player has Air element
        var pd = elementManager.data(player.getUniqueId());
        if (pd == null || pd.getCurrentElement() != ElementType.AIR) return;

        // Cancel fall damage for Air element players
        e.setCancelled(true);
    }
}