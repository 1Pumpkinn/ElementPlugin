package hs.elementPlugin.items;

import hs.elementPlugin.ElementPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class RerollerItem {
    private RerollerItem() {}

    public static final String KEY = "element_reroller";

    public static ItemStack make(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.HEART_OF_THE_SEA);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Element Reroller");
        meta.setLore(List.of(
            ChatColor.GRAY + "Allows you to change your element",
            ChatColor.YELLOW + "Right-click to randomly reroll your element"
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.reroller(plugin), PersistentDataType.BYTE, (byte)1);
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
            recipe.shape("IEG", "ETE", "DEM");
            recipe.setIngredient('I', Material.IRON_BLOCK);
            recipe.setIngredient('G', Material.GOLD_BLOCK);
            recipe.setIngredient('D', Material.DIAMOND_BLOCK);
            recipe.setIngredient('M', Material.EMERALD_BLOCK);
            recipe.setIngredient('E', Material.NETHERITE_SCRAP);
            recipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
            
            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Element Reroller recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Element Reroller recipe: " + e.getMessage());
        }
    }
}