package hs.elementPlugin.recipes.water;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class WaterRecipes {
    private static final String WATER_ELEMENT_KEY = "water_element";

    public static void registerRecipes(ElementPlugin plugin) {
        registerWaterElementRecipe(plugin);
    }

    private static void registerWaterElementRecipe(ElementPlugin plugin) {
        try {
            ItemStack result = createWaterElementItem(plugin);
            NamespacedKey key = new NamespacedKey(plugin, WATER_ELEMENT_KEY);
            
            plugin.getServer().removeRecipe(key);
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("WWW", "WDW", "WWW");
            recipe.setIngredient('W', Material.WATER_BUCKET);
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            
            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Water Element recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Water Element recipe: " + e.getMessage());
        }
    }


    private static ItemStack createWaterElementItem(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.WATER_BUCKET);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.BLUE + "Water Element");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.elementType(plugin), PersistentDataType.STRING, "WATER");
        pdc.set(ItemKeys.elementItem(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

}
