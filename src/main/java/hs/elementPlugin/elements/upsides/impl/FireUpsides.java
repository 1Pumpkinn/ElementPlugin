package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class FireUpsides extends BaseUpsides {
    private static final Map<Material, Material> ORE_SMELTING_MAP = new HashMap<>();
    
    static {
        // Coal ores
        ORE_SMELTING_MAP.put(Material.COAL_ORE, Material.COAL);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_COAL_ORE, Material.COAL);
        
        // Iron ores
        ORE_SMELTING_MAP.put(Material.IRON_ORE, Material.IRON_INGOT);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_IRON_ORE, Material.IRON_INGOT);
        
        // Copper ores
        ORE_SMELTING_MAP.put(Material.COPPER_ORE, Material.COPPER_INGOT);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_COPPER_ORE, Material.COPPER_INGOT);
        
        // Gold ores
        ORE_SMELTING_MAP.put(Material.GOLD_ORE, Material.GOLD_INGOT);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_GOLD_ORE, Material.GOLD_INGOT);
        ORE_SMELTING_MAP.put(Material.NETHER_GOLD_ORE, Material.GOLD_NUGGET);
        
        // Redstone ores
        ORE_SMELTING_MAP.put(Material.REDSTONE_ORE, Material.REDSTONE);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_REDSTONE_ORE, Material.REDSTONE);
        
        // Lapis ores
        ORE_SMELTING_MAP.put(Material.LAPIS_ORE, Material.LAPIS_LAZULI);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_LAPIS_ORE, Material.LAPIS_LAZULI);
        
        // Diamond ores
        ORE_SMELTING_MAP.put(Material.DIAMOND_ORE, Material.DIAMOND);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_DIAMOND_ORE, Material.DIAMOND);
        
        // Emerald ores
        ORE_SMELTING_MAP.put(Material.EMERALD_ORE, Material.EMERALD);
        ORE_SMELTING_MAP.put(Material.DEEPSLATE_EMERALD_ORE, Material.EMERALD);
        
        // Quartz ores
        ORE_SMELTING_MAP.put(Material.NETHER_QUARTZ_ORE, Material.QUARTZ);
    }

    public FireUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.FIRE;
    }

    /**
     * Apply all Fire element upsides to a player
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Fire element
     */
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Infinite Fire Resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
    }

    /**
     * Check if player should be immune to fire/lava damage
     * @param player The player taking damage
     * @param damageCause The cause of the damage
     * @return true if fire/lava damage should be cancelled
     */
    public boolean shouldCancelFireDamage(Player player, EntityDamageEvent.DamageCause damageCause) {
        if (!hasElement(player)) {
            return false;
        }
        
        return damageCause == EntityDamageEvent.DamageCause.FIRE ||
               damageCause == EntityDamageEvent.DamageCause.FIRE_TICK ||
               damageCause == EntityDamageEvent.DamageCause.LAVA;
    }

    /**
     * Check if ores in player's inventory should be instantly smelted
     * @param player The player with the inventory
     * @return true if ores should be smelted
     */
    public boolean shouldSmeltOresInInventory(Player player) {
        return hasElement(player) && getUpgradeLevel(player) >= 2;
    }

    /**
     * Smelt all ores in player's inventory instantly
     * @param player The player whose inventory to smelt
     */
    public void smeltOresInInventory(Player player) {
        if (!shouldSmeltOresInInventory(player)) return;
        
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                Material smeltedType = ORE_SMELTING_MAP.get(item.getType());
                if (smeltedType != null) {
                    // Create new ItemStack with smelted material to avoid deprecation warning
                    ItemStack smeltedItem = new ItemStack(smeltedType, item.getAmount());
                    smeltedItem.setItemMeta(item.getItemMeta());
                    player.getInventory().setItem(player.getInventory().first(item), smeltedItem);
                }
            }
        }
    }

    /**
     * Check if a material is an ore that can be smelted
     * @param material The material to check
     * @return true if it's a smeltable ore
     */
    public boolean isSmeltableOre(Material material) {
        return ORE_SMELTING_MAP.containsKey(material);
    }

    /**
     * Get the smelted result of an ore
     * @param oreType The ore type
     * @return The smelted material, or null if not smeltable
     */
    public Material getSmeltedResult(Material oreType) {
        return ORE_SMELTING_MAP.get(oreType);
    }
}
