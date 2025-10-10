package hs.elementPlugin.recipes.death;

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

public class DeathRecipes {
    private static final String DEATH_ELEMENT_KEY = "death_element";

    public static void registerRecipes(ElementPlugin plugin) {
        registerDeathElementRecipe(plugin);
    }

    private static void registerDeathElementRecipe(ElementPlugin plugin) {
        try {
            ItemStack result = createDeathElementItem(plugin);
            NamespacedKey key = new NamespacedKey(plugin, DEATH_ELEMENT_KEY);
            
            plugin.getServer().removeRecipe(key);
            
            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape("WSW", "SDS", "WSW");
            recipe.setIngredient('W', Material.WITHER_SKELETON_SKULL);
            recipe.setIngredient('S', Material.SOUL_SAND);
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            
            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Death Element recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Death Element recipe: " + e.getMessage());
        }
    }

    private static ItemStack createDeathElementItem(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.WITHER_SKELETON_SKULL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Death Element");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.elementType(plugin), PersistentDataType.STRING, "DEATH");
        pdc.set(ItemKeys.elementItem(plugin), PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }
}
