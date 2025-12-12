package hs.elementPlugin.items.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class Upgrader2Item {
    private Upgrader2Item() {}

    public static final String KEY = "upgrader_2";

    public static ItemStack make(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bUpgrader II");
        meta.setLore(List.of("Use by crafting to unlock", "Ability 2 + Upside 2 for your element"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.upgraderLevel(plugin), PersistentDataType.INTEGER, 2);
        item.setItemMeta(meta);
        return item;
    }

    public static void registerRecipe(ElementPlugin plugin) {
        try {
            ItemStack result = make(plugin);
            NamespacedKey key = new NamespacedKey(plugin, KEY);
            
            // Remove existing recipe if it exists
            plugin.getServer().removeRecipe(key);
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("DGD", "NWN", "DGD");
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            recipe.setIngredient('G', Material.GOLD_BLOCK);
            recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
            recipe.setIngredient('N', Material.NETHERITE_INGOT);


            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Upgrader II recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Upgrader II recipe: " + e.getMessage());
        }
    }
}