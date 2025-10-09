package hs.elementPlugin.elements.impl.air.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AirCombatListener implements Listener {
    private final ElementManager elementManager;

    public AirCombatListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        
        // Apply Air Upside 2: 5% chance to give slow falling 5s to victim when hit
        var pd = elementManager.data(damager.getUniqueId());
        if (pd.getCurrentElement() == ElementType.AIR && pd.getUpgradeLevel(ElementType.AIR) >= 2) {
            if (Math.random() < 0.05) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 5 * 20, 0, true, true, true));
            }
        }
    }
}
