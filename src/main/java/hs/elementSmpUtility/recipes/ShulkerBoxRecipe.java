package hs.elementSmpUtility.recipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Recipe for crafting shulker boxes
 * Pattern: 2 shulker shells with chest in between
 */
public class ShulkerBoxRecipe {

    /**
     * Register the shulker box crafting recipe
     *
     * @param plugin The plugin instance
     */
    public static void register(JavaPlugin plugin) {
        ItemStack shulkerBox = new ItemStack(Material.SHULKER_BOX, 1);

        NamespacedKey key = new NamespacedKey(plugin, "shulker_box_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, shulkerBox);


        recipe.shape(
                " S ",
                "SCS",
                " S "
        );

        recipe.setIngredient('S', Material.BARREL);
        recipe.setIngredient('C', Material.BUNDLE);

        try {
            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("Registered Shulker Box crafting recipe (2x Shulker Shell + 1x Chest)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register shulker box recipe: " + e.getMessage());
        }
    }
}