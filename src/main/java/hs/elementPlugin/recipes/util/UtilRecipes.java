package hs.elementPlugin.recipes.util;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.AdvancedRerollerItem;
import hs.elementPlugin.items.Upgrader1Item;
import hs.elementPlugin.items.Upgrader2Item;
import hs.elementPlugin.items.RerollerItem;

public class UtilRecipes {
    public static void registerRecipes(ElementPlugin plugin) {
        Upgrader1Item.registerRecipe(plugin);
        Upgrader2Item.registerRecipe(plugin);
        RerollerItem.registerRecipe(plugin);
        AdvancedRerollerItem.registerRecipe(plugin);
    }
}
