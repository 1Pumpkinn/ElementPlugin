package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Frost element
     */
    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Speed 2 when wearing leather boots
        if (isWearingLeatherBoots(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, false));
        }

        // Upside 2: Speed 3 on ice (requires upgrade level 2)
        if (upgradeLevel >= 2 && isOnIce(player)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2, true, false, false));
        }
    }

    /**
     * Check if player is wearing leather boots
     * @param player The player to check
     * @return true if wearing leather boots
     */
    public boolean isWearingLeatherBoots(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        return boots != null && boots.getType() == Material.LEATHER_BOOTS;
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
     * Check if player should get speed 2 from leather boots
     * @param player The player to check
     * @return true if effect should be applied
     */
    public boolean shouldApplyLeatherBootsSpeed(Player player) {
        return hasElement(player) && isWearingLeatherBoots(player);
    }

    /**
     * Check if player should get speed 3 from ice
     * @param player The player to check
     * @return true if effect should be applied
     */
    public boolean shouldApplyIceSpeed(Player player) {
        return hasElement(player) && getUpgradeLevel(player) >= 2 && isOnIce(player);
    }
}