package hs.elementPlugin.recipes.life;

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

public class LifeRecipes {
    private static final String LIFE_ELEMENT_KEY = "life_element";

    public static void registerRecipes(ElementPlugin plugin) {
        registerLifeElementRecipe(plugin);
    }

    private static void registerLifeElementRecipe(ElementPlugin plugin) {
        try {
            ItemStack result = createLifeElementItem(plugin);
            NamespacedKey key = new NamespacedKey(plugin, LIFE_ELEMENT_KEY);
            
            plugin.getServer().removeRecipe(key);
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("DND", "NTN", "DND");
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            recipe.setIngredient('N', Material.NETHERITE_INGOT);
            recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
            
            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Life Element recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Life Element recipe: " + e.getMessage());
        }
    }


    private static ItemStack createLifeElementItem(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Life Element");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.elementType(plugin), PersistentDataType.STRING, "LIFE");
        pdc.set(ItemKeys.elementItem(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

}
