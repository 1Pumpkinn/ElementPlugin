package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AirUpsides extends BaseUpsides {

    public AirUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.AIR;
    }

    /**
     * Apply all Air element upsides to a player
     *
     * @param player       The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Air element
     */
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: No fall damage (handled in FallDamageListener)
        // No potion effects needed for this upside
    }

    /**
     * Check if player should apply slow falling to victim when hitting them
     *
     * @param player The Air element player
     * @return true if slow falling should be applied (5% chance at upgrade level 2+)
     */
    public boolean shouldApplySlowFallingToVictim(Player player) {
        if (!hasElement(player) || getUpgradeLevel(player) < 2) {
            return false;
        }
        return Math.random() < 0.05; // 5% chance
    }

    /**
     * Apply slow falling effect to a victim
     *
     * @param victim The player to apply slow falling to
     */
    public void applySlowFallingToVictim(Player victim) {
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 5 * 20, 0, true, true, true));
    }
}
