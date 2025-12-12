package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FrostUpsides extends BaseUpsides {

    public FrostUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.FROST;
    }

    /**
     * Apply all Frost element upsides to a player
     * Upside 1: Speed III on ice (always active)
     * Upside 2: Freeze on hit (35% chance, Upgrade II) - handled in FrostCombatListener
     *
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Frost element
     */
    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Speed III on ice (handled by FrostPassiveListener)
        // No potion effect needed here - it's applied dynamically

        // Upside 2: Freeze on hit (35% chance) - requires Upgrade II
        // This is handled passively in FrostCombatListener
        // No potion effect needed here
    }

    /**
     * Check if player is standing on ice
     * @param player The player to check
     * @return true if standing on ice blocks
     */
    public boolean isOnIce(Player player) {
        Material blockBelow = player.getLocation().subtract(0, 1, 0).getBlock().getType();
        return blockBelow == Material.ICE ||
                blockBelow == Material.PACKED_ICE ||
                blockBelow == Material.BLUE_ICE ||
                blockBelow == Material.FROSTED_ICE;
    }

    /**
     * Check if player should get speed 3 from ice (Upside 1)
     * @param player The player to check
     * @return true if effect should be applied
     */
    public boolean shouldApplyIceSpeed(Player player) {
        return hasElement(player) && isOnIce(player);
    }

    /**
     * Check if player should apply freeze effect on hit (Upside 2)
     * Requires Upgrade 2
     * @param player The Frost element player
     * @return true if freeze should be applied (35% chance)
     */
    public boolean shouldApplyFreezeOnHit(Player player) {
        if (!hasElement(player) || getUpgradeLevel(player) < 2) {
            return false;
        }
        return Math.random() < 0.35; // 35% chance
    }
}