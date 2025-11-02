package hs.elementSmpUtility.recipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Recipe for crafting cobwebs from string
 * Pattern: 3x3 grid of string = 1 cobweb
 */
public class CobwebRecipe {

    /**
     * Register the cobweb crafting recipe
     *
     * @param plugin The plugin instance
     */
    public static void register(JavaPlugin plugin) {
        ItemStack cobweb = new ItemStack(Material.COBWEB, 1);

        NamespacedKey key = new NamespacedKey(plugin, "cobweb_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, cobweb);


        recipe.shape(
                "S S",
                " S ",
                "S S"
        );

        recipe.setIngredient('S', Material.STRING);

        try {
            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("Registered Cobweb crafting recipe (9x String)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register cobweb recipe: " + e.getMessage());
        }
    }
}