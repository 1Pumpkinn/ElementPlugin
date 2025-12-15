package saturn.elementPlugin.elements.upsides.impl;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.upsides.BaseUpsides;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MetalUpsides extends BaseUpsides {

    public MetalUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.METAL;
    }

    /**
     * Apply all Metal element upsides to a player
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Metal element
     */
    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Haste 1 permanently
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));

        // Upside 2: Reduced knockback (handled in MetalKnockbackListener)
        // No passive effect needed here
    }

    /**
     * Check if player should have reduced knockback (Upside 2)
     * Requires Upgrade 2
     *
     * @param player The Metal element player
     * @return true if knockback should be reduced
     */
    public boolean shouldReduceKnockback(Player player) {
        return hasElement(player) && getUpgradeLevel(player) >= 2;
    }
}