package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumSet;
import java.util.Set;

public class EarthUpsides extends BaseUpsides {
    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
    );

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
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Permanent Hero of the Village 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
    }

    /**
     * Check if ore drops should be doubled for this player
     * @param player The player mining the ore
     * @param oreType The type of ore being mined
     * @return true if ore drops should be doubled
     */
    public boolean shouldDoubleOreDrops(Player player, Material oreType) {
        if (!hasElement(player) || getUpgradeLevel(player) < 2) {
            return false;
        }

        // Only double drops if NOT using silk touch
        if (player.getInventory().getItemInMainHand().getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
            return false;
        }

        return ORES.contains(oreType);
    }

    /**
     * Check if the player is using silk touch
     * @param player The player to check
     * @return true if the player has silk touch on their main hand item
     */
    public boolean isUsingSilkTouch(Player player) {
        return player.getInventory().getItemInMainHand().getEnchantments().containsKey(Enchantment.SILK_TOUCH);
    }

    /**
     * Get the multiplier for ore drops (1.0 for normal, 2.0 for doubled)
     * @param player The player mining the ore
     * @param oreType The type of ore being mined
     * @return The drop multiplier
     */
    public double getOreDropMultiplier(Player player, Material oreType) {
        return shouldDoubleOreDrops(player, oreType) ? 2.0 : 1.0;
    }
}