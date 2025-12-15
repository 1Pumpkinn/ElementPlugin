package saturn.elementPlugin.elements.upsides.impl;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.upsides.BaseUpsides;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterUpsides extends BaseUpsides {

    public WaterUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.WATER;
    }

    /**
     * Apply all Water element upsides to a player
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Water element
     */
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Conduit Power (permanent water breathing)
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));

        // Upside 2: Mine faster in water - requires upgrade level 2
        if (upgradeLevel >= 2) {
            AttributeInstance attr = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED);
            if (attr != null) {
                // Default is 0.2 (5x slower underwater)
                // Set to 1.2 (slightly faster than on land)
                attr.setBaseValue(1.2);
            }
        }
    }
    /**
     * Reset water mining speed to default
     * @param player The player to reset
     */
    public void resetMiningSpeed(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED);
        if (attr != null) {
            // Reset to default underwater mining speed
            attr.setBaseValue(0.2);
        }
    }
}