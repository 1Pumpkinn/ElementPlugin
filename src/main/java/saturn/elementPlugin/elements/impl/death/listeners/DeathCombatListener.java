package saturn.elementPlugin.elements.impl.death.listeners;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import saturn.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
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
 * FIXED: Now handles both melee and projectile attacks
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
        // Get the attacker (handles both melee and projectile)
        Player attacker = null;

        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player) {
                attacker = (Player) projectile.getShooter();
            }
        }

        if (attacker == null) {
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
    }
}
