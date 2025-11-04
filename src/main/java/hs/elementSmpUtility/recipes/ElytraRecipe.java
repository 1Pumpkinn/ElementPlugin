package hs.elementSmpUtility.recipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Recipe for crafting Elytra
 * Uses Phantom Membranes and Feathers in a wing-like pattern
 */
public class ElytraRecipe {

    public static void register(JavaPlugin plugin) {
        ItemStack elytra = new ItemStack(Material.ELYTRA, 1);

        NamespacedKey key = new NamespacedKey(plugin, "elytra_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, elytra);

        // Wing-like pattern
        recipe.shape(
                "NAN",
                "PEP",
                "N N"
        );

        recipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
        recipe.setIngredient('A', Material.AMETHYST_SHARD);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('E', Material.ENDER_EYE);

        try {
            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("Registered Elytra crafting recipe (6x Phantom Membrane + 4x Feather + 1x Diamond)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register Elytra recipe: " + e.getMessage());
        }
    }
}