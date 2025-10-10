package hs.elementPlugin.recipes.earth;

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

public class EarthRecipes {
    private static final String EARTH_ELEMENT_KEY = "earth_element";

    public static void registerRecipes(ElementPlugin plugin) {
        registerEarthElementRecipe(plugin);
    }

    private static void registerEarthElementRecipe(ElementPlugin plugin) {
        try {
            ItemStack result = createEarthElementItem(plugin);
            NamespacedKey key = new NamespacedKey(plugin, EARTH_ELEMENT_KEY);
            
            plugin.getServer().removeRecipe(key);
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("GGG", "GDG", "GGG");
            recipe.setIngredient('G', Material.GRASS_BLOCK);
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            
            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Earth Element recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Earth Element recipe: " + e.getMessage());
        }
    }


    private static ItemStack createEarthElementItem(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Earth Element");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.elementType(plugin), PersistentDataType.STRING, "EARTH");
        pdc.set(ItemKeys.elementItem(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

}
