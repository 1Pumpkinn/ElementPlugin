package saturn.elementPlugin.elements.impl.death.listeners;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import saturn.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import saturn.elementPlugin.managers.ElementManager;
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
 * - Death Slash ability (bleeding) - Ability 1
 * - Death Clock ability (curse) - Ability 2
 * - Wither on hit (10% chance, Upgrade II passive)
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


        // Check for ability activations first
        boolean deathClock = attacker.hasMetadata(DeathClockAbility.META_DEATH_CLOCK_ACTIVE);
        boolean slash = attacker.hasMetadata(DeathSlashAbility.META_SLASH_ACTIVE);

        if (deathClock) {
            DeathClockAbility.applyEffects(plugin, attacker, target);
        }

        if (slash) {
            DeathSlashAbility.applyBleeding(plugin, attacker, target);
        }

        // UPDATED: Apply wither on hit (10% chance) if player has Upgrade II
        if (data.getUpgradeLevel(ElementType.DEATH) >= 2) {
            if (Math.random() < 0.10) { // 10% chance
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