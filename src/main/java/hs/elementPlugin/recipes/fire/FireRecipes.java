package hs.elementPlugin.recipes.fire;

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

public class FireRecipes {
    private static final String FIRE_ELEMENT_KEY = "fire_element";

    public static void registerRecipes(ElementPlugin plugin) {
        registerFireElementRecipe(plugin);
    }

    private static void registerFireElementRecipe(ElementPlugin plugin) {
        try {
            ItemStack result = createFireElementItem(plugin);
            NamespacedKey key = new NamespacedKey(plugin, FIRE_ELEMENT_KEY);
            
            plugin.getServer().removeRecipe(key);
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("FFF", "FDF", "FFF");
            recipe.setIngredient('F', Material.FIRE_CHARGE);
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            
            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Fire Element recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Fire Element recipe: " + e.getMessage());
        }
    }


    private static ItemStack createFireElementItem(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta meta = item.getItemMeta();

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.elementType(plugin), PersistentDataType.STRING, "FIRE");
        pdc.set(ItemKeys.elementItem(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

}
