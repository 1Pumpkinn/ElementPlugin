package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
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
        // Upside 1: Resistance 1 permanently
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, Integer.MAX_VALUE, 0, true, false));

        // Upside 2: Armor breaks slower (handled in MetalArmorDurabilityListener)
        // No passive effect needed here
    }

    /**
     * Check if player should have armor durability reduction (Upside 2)
     * Requires Upgrade 2
     *
     * @param player The Metal element player
     * @return true if armor should break slower
     */
    public boolean shouldReduceArmorDurability(Player player) {
        return hasElement(player) && getUpgradeLevel(player) >= 2;
    }
}