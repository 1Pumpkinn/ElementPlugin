package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireUpsides extends BaseUpsides {



    public FireUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.FIRE;
    }

    /**
     * Apply all Fire element upsides to a player
     * Upside 1: Infinite Fire Resistance
     * Upside 2: Fire Aspect on hits (handled in FireCombatListener)
    */

    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Infinite Fire Resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));

        // Upside 2: Fire Aspect on hits (requires Upgrade 2)
        // This is handled passively in FireCombatListener
        // No potion effect needed here
    }

    /**
     * Check if player should be immune to fire/lava damage
     *
     * @param player      The player taking damage
     * @param damageCause The cause of the damage
     * @return true if fire/lava damage should be cancelled
     */
    public boolean shouldCancelFireDamage(Player player, EntityDamageEvent.DamageCause damageCause) {
        if (!hasElement(player)) {
            return false;
        }

        return damageCause == EntityDamageEvent.DamageCause.FIRE ||
                damageCause == EntityDamageEvent.DamageCause.FIRE_TICK ||
                damageCause == EntityDamageEvent.DamageCause.LAVA;
    }

    /**
     * Check if player should apply fire aspect to enemies (Upside 2)
     * Requires Upgrade 2
     *
     * @param player The Fire element player
     * @return true if fire aspect should be applied
     */
    public boolean shouldApplyFireAspect(Player player) {
        return hasElement(player) && getUpgradeLevel(player) >= 2;
    }
}


