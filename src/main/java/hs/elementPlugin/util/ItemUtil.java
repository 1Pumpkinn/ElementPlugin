package hs.elementPlugin.util;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class ItemUtil {
    private ItemUtil() {}

    public static boolean isElementItem(ElementPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        Byte flag = stack.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE);
        return flag != null && flag == (byte)1;
    }

    public static ElementType getElementType(ElementPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        String t = stack.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING);
        if (t == null) return null;
        try { return ElementType.valueOf(t); } catch (IllegalArgumentException ex) { return null; }
    }

    public static boolean isHoldingElementItem(ElementPlugin plugin, Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        return isElementItem(plugin, main) || isElementItem(plugin, off);
    }
}