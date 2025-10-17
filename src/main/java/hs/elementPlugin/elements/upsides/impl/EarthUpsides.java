package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EarthUpsides extends BaseUpsides {

    public EarthUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.EARTH;
    }

    /**
     * Apply all Earth element upsides to a player
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Earth element
     */
    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Permanent Hero of the Village 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));

    }
}