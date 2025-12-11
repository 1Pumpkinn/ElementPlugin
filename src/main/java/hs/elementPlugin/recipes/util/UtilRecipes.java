package hs.elementPlugin.recipes.util;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.impl.AdvancedRerollerItem;
import hs.elementPlugin.items.impl.RerollerItem;
import hs.elementPlugin.items.impl.Upgrader1Item;
import hs.elementPlugin.items.impl.Upgrader2Item;

public class UtilRecipes {
    public static void registerRecipes(ElementPlugin plugin) {
        Upgrader1Item.registerRecipe(plugin);
        Upgrader2Item.registerRecipe(plugin);
        RerollerItem.registerRecipe(plugin);
        AdvancedRerollerItem.registerRecipe(plugin);
    }
}
