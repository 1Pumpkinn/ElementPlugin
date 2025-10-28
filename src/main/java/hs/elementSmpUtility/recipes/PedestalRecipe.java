package hs.elementSmpUtility.recipes;

import hs.elementSmpUtility.blocks.CustomBlockManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Recipe for crafting the Pedestal block
 * Lodestone with Ender Pearl on top
 */
public class PedestalRecipe {

    private final JavaPlugin plugin;
    private final CustomBlockManager blockManager;

    public PedestalRecipe(JavaPlugin plugin, CustomBlockManager blockManager) {
        this.plugin = plugin;
        this.blockManager = blockManager;
    }

    /**
     * Register the pedestal crafting recipe
     */
    public void register() {
        ItemStack pedestalItem = blockManager.createCustomBlock("pedestal", 1);
        if (pedestalItem == null) {
            plugin.getLogger().warning("Failed to create pedestal item for recipe!");
            return;
        }

        NamespacedKey key = new NamespacedKey(plugin, "pedestal");
        ShapedRecipe recipe = new ShapedRecipe(key, pedestalItem);

        // Recipe pattern:
        //  [ ][ ][ ]
        //  [ ][E][ ]
        //  [ ][L][ ]
        // E = Ender Pearl
        // L = Lodestone
        recipe.shape(
                "   ",
                " E ",
                " L "
        );

        recipe.setIngredient('E', Material.ENDER_PEARL);
        recipe.setIngredient('L', Material.LODESTONE);

        try {
            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("Registered Pedestal crafting recipe (Lodestone + Ender Pearl)");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register pedestal recipe: " + e.getMessage());
        }
    }
}