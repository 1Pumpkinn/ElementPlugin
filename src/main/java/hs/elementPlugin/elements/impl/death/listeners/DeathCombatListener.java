package hs.elementPlugin.elements.impl.death.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener for Death element combat abilities (Death Clock and Slash)
 */
public class DeathCombatListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public DeathCombatListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        var data = elementManager.data(attacker.getUniqueId());
        if (data == null || data.getCurrentElement() != ElementType.DEATH) {
            return;
        }

        if (target instanceof Player targetPlayer) {
            if (plugin.getTrustManager().isTrusted(attacker.getUniqueId(), targetPlayer.getUniqueId())) {
                return;
            }
        }

        boolean deathClock = attacker.hasMetadata(DeathClockAbility.META_DEATH_CLOCK_ACTIVE);
        boolean slash = attacker.hasMetadata(DeathSlashAbility.META_SLASH_ACTIVE);

        if (deathClock) {
            DeathClockAbility.applyEffects(plugin, attacker, target);
        }

        if (slash) {
            DeathSlashAbility.applyBleeding(plugin, attacker, target);
        }
    }
}
