package saturn.elementPlugin.util;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.items.ItemKeys;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public final class ItemUtil {
    private ItemUtil() {}

    public static boolean isElementItem(ElementPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return false;
        Byte flag = stack.getItemMeta().getPersistentDataContainer()
                .get(ItemKeys.elementItem(plugin), PersistentDataType.BYTE);
        return flag != null && flag == (byte)1;
    }

    public static ElementType getElementType(ElementPlugin plugin, ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        String t = stack.getItemMeta().getPersistentDataContainer()
                .get(ItemKeys.elementType(plugin), PersistentDataType.STRING);
        if (t == null) return null;
        try { return ElementType.valueOf(t); } catch (IllegalArgumentException ex) { return null; }
    }
}