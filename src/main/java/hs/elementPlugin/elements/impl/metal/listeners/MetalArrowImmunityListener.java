package hs.elementPlugin.elements.impl.metal.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MetalArrowImmunityListener implements Listener {
    private final ElementManager elementManager;

    public MetalArrowImmunityListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!(event.getDamager() instanceof Arrow)) return;

        // Check if player has Metal element with Upgrade 2
        var pd = elementManager.data(player.getUniqueId());
        if (pd.getCurrentElement() == ElementType.METAL && pd.getUpgradeLevel(ElementType.METAL) >= 2) {
            event.setCancelled(true);
        }
    }
}