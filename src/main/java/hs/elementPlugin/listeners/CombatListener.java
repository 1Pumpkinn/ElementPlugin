package hs.elementPlugin.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;

public class CombatListener implements Listener {
    private final TrustManager trust;
    private final ElementManager elements;

    public CombatListener(TrustManager trust, ElementManager elements) {
        this.trust = trust;
        this.elements = elements;
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player damager = null;
        if (e.getDamager() instanceof Player p) {
            damager = p;
        } else if (e.getDamager() instanceof org.bukkit.entity.Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
        }
        if (damager == null) return;
        if (trust.isTrusted(victim.getUniqueId(), damager.getUniqueId()) || trust.isTrusted(damager.getUniqueId(), victim.getUniqueId())) {
            e.setCancelled(true);
        }
        // Apply Air Upside 2: 5% chance to give slow falling 5s to victim when hit
        var pd = elements.data(damager.getUniqueId());
        if (pd.getCurrentElement() == ElementType.AIR && pd.getUpgradeLevel(ElementType.AIR) >= 2) {
            if (Math.random() < 0.05) {
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOW_FALLING, 5 * 20, 0, true, true, true));
            }
        }
    }
}