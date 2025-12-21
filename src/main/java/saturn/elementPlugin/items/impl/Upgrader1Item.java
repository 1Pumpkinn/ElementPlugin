package saturn.elementPlugin.items.impl;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.items.ItemKeys;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class Upgrader1Item {
    private Upgrader1Item() {}

    public static final String KEY = "upgrader_1";

    public static ItemStack make(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§aUpgrader I");
        meta.setLore(List.of("Use by crafting to unlock", "Ability 1 for your element"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(ItemKeys.upgraderLevel(plugin), PersistentDataType.INTEGER, 1);
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
            recipe.shape("ELE", "LSL", "ELE");
            recipe.setIngredient('E', Material.EMERALD_BLOCK);
            recipe.setIngredient('L', Material.LAPIS_BLOCK);
            recipe.setIngredient('S', Material.NAUTILUS_SHELL);



            boolean success = plugin.getServer().addRecipe(recipe);
            if (!success) {
                plugin.getLogger().warning("Failed to register Upgrader I recipe");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error registering Upgrader I recipe: " + e.getMessage());
        }
    }
}