package saturn.elementPlugin.recipes.util;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.items.impl.AdvancedRerollerItem;
import saturn.elementPlugin.items.impl.RerollerItem;
import saturn.elementPlugin.items.impl.Upgrader1Item;
import saturn.elementPlugin.items.impl.Upgrader2Item;

/**
 * Utility class for registering all utility item recipes
 * Called during plugin startup to add recipes to the server
 */
public class UtilRecipes {


    public static void registerRecipes(ElementPlugin plugin) {
        // Register upgrader recipes
        Upgrader1Item.registerRecipe(plugin);
        Upgrader2Item.registerRecipe(plugin);

        // Register reroller recipes (CRITICAL - both rerollers registered)
        RerollerItem.registerRecipe(plugin);
        AdvancedRerollerItem.registerRecipe(plugin);
    }
}