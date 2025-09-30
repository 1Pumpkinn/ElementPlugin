package hs.elementPlugin.items;

import hs.elementPlugin.ElementPlugin;
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
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_UPGRADER_LEVEL), PersistentDataType.INTEGER, 1);
        item.setItemMeta(meta);
        return item;
    }

    public static void registerRecipe(ElementPlugin plugin) {
        ItemStack result = make(plugin);
        NamespacedKey key = new NamespacedKey(plugin, KEY);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("III", "ISI", "III");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('S', Material.AMETHYST_SHARD);
        plugin.getServer().addRecipe(recipe);
    }
}