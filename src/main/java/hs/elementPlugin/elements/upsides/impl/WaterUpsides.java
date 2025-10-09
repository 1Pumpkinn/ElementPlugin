package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
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
        // Upside 1: Infinite Conduit Power 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
        
        // Upside 2: Dolphins Grace 5 permanently (upgrade level 2+)
        if (upgradeLevel >= 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 4, true, false));
        }
    }
}
