package hs.elementPlugin.elements.impl.death.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Listener for Death element combat abilities and passive effects
 * - Death Clock ability (on-hit curse)
 * - Death Slash ability (bleeding)
 * - Wither on hit (35% chance, Upgrade II passive)
 */
public class DeathCombatListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;
    private final TrustManager trustManager;

    public DeathCombatListener(ElementPlugin plugin, ElementManager elementManager, TrustManager trustManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        this.trustManager = trustManager;
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

        // Don't apply to trusted players (check both directions)
        if (target instanceof Player victim) {
            if (trustManager.isTrusted(attacker.getUniqueId(), victim.getUniqueId()) ||
                    trustManager.isTrusted(victim.getUniqueId(), attacker.getUniqueId())) {
                return;
            }
        }

        // Check for ability activations first
        boolean deathClock = attacker.hasMetadata(DeathClockAbility.META_DEATH_CLOCK_ACTIVE);
        boolean slash = attacker.hasMetadata(DeathSlashAbility.META_SLASH_ACTIVE);

        if (deathClock) {
            DeathClockAbility.applyEffects(plugin, attacker, target);
        }

        if (slash) {
            DeathSlashAbility.applyBleeding(plugin, attacker, target);
        }

        // FIXED: Apply wither on hit (35% chance) if player has Upgrade II
        if (data.getUpgradeLevel(ElementType.DEATH) >= 2) {
            if (Math.random() < 0.35) { // 35% chance
                // Apply Wither II for 4 seconds (80 ticks)
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.WITHER,
                        80, // 4 seconds
                        1, // Wither II
                        false,
                        true,
                        true
                ));
            }
        }
    }
}