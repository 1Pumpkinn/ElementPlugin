package hs.elementPlugin.items.life;

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

public final class LifeItem {
    private LifeItem() {}

    public static final String KEY = "life_element";

    public static ItemStack make(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Life Element");
        meta.setLore(List.of(
            ChatColor.GRAY + "A rare element of healing",
            ChatColor.GRAY + "Can only be crafted once per server",
            ChatColor.YELLOW + "Upside 1: 15 Hearts permanently",
            ChatColor.YELLOW + "Ability 1: Regeneration aura (50 mana)",
            ChatColor.YELLOW + "Ability 2: Healing beam (75 mana)",
            ChatColor.YELLOW + "Upside 2: Crops grow in 5x5 radius"
        ));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING, "LIFE");
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
}
