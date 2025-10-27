package hs.elementPlugin.listeners.items.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ElementInventoryProtectionListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;

    public ElementInventoryProtectionListener(ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        Inventory top = event.getView().getTopInventory();

        // Only check for Ender Chest
        if (top == null || top.getType() != InventoryType.ENDER_CHEST) return;

        // Check for Life/Death cores in Ender Chest
        if ((cursor != null && isLifeOrDeathCore(cursor)) || (current != null && isLifeOrDeathCore(current))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot store Life or Death cores in an Ender Chest!");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack item = event.getOldCursor();
        Inventory top = event.getView().getTopInventory();

        // Only check for Ender Chest
        if (top == null || top.getType() != InventoryType.ENDER_CHEST) return;

        // Check for Life/Death cores in Ender Chest
        if (item != null && isLifeOrDeathCore(item)) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "You cannot store Life or Death cores in an Ender Chest!");
        }
    }

    private boolean isLifeOrDeathCore(ItemStack stack) {
        if (!ItemUtil.isElementItem(plugin, stack)) return false;
        ElementType type = ItemUtil.getElementType(plugin, stack);
        return type == ElementType.LIFE || type == ElementType.DEATH;
    }
}