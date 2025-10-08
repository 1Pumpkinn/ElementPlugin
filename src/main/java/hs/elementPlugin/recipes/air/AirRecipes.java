package hs.elementPlugin.recipes.air;

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

public class AirRecipes {
    private static final String AIR_ELEMENT_KEY = "air_element";

    public static void registerRecipes(ElementPlugin plugin) {
        registerAirElementRecipe(plugin);
    }

    private static void registerAirElementRecipe(ElementPlugin plugin) {
        try {
            ItemStack result = createAirElementItem(plugin);
            NamespacedKey key = new NamespacedKey(plugin, AIR_ELEMENT_KEY);
            
            plugin.getServer().removeRecipe(key);
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("FFF", "FDF", "FFF");
            recipe.setIngredient('F', Material.FEATHER);
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            
            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Air Element recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Air Element recipe: " + e.getMessage());
        }
    }


    private static ItemStack createAirElementItem(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.FEATHER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Air Element");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING, "AIR");
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

}
