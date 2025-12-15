package saturn.elementPlugin.listeners.core;

import saturn.elementPlugin.managers.ElementManager;
import saturn.elementPlugin.managers.TeamManager;import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class CombatListener implements Listener {
    private final TeamManager trust;
    private final ElementManager elements;

    public CombatListener(TeamManager trust, ElementManager elements) {
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
    }
}