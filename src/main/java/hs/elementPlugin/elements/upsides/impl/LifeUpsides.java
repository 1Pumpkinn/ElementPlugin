package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;

public class LifeUpsides extends BaseUpsides {

    public LifeUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.LIFE;
    }

    /**
     * Apply all Life element upsides to a player
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Life element
     */
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Permanently 15 Hearts (30 HP) instead of 20 HP
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) player.setHealth(attr.getBaseValue());
        }
    }

    /**
     * Check if crops should grow instantly around this player
     * @param player The Life element player
     * @return true if crops should grow instantly
     */
    public boolean shouldGrowCropsInstantly(Player player) {
        return hasElement(player) && getUpgradeLevel(player) >= 2;
    }

    /**
     * Grow a crop block if it's a valid crop
     * @param block The block to potentially grow
     */
    public void growIfCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) {
                ageable.setAge(ageable.getMaximumAge());
                block.setBlockData(ageable);
            }
        }
    }

    /**
     * Reset player's max health to default (20 HP)
     * @param player The player to reset
     */
    public void resetMaxHealth(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) player.setHealth(20.0);
        }
    }
}
