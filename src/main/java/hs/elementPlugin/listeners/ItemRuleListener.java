package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ItemRuleListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;
    private final ManaManager mana;
    private final hs.elementPlugin.managers.ItemManager itemManager;

    public ItemRuleListener(ElementPlugin plugin, ElementManager elements, ManaManager mana, hs.elementPlugin.managers.ItemManager itemManager) {
        this.plugin = plugin;
        this.elements = elements;
        this.mana = mana;
        this.itemManager = itemManager;
    }

    private boolean isElementItem(ItemStack stack) {
        return ItemUtil.isElementItem(plugin, stack);
    }

    private ElementType getElementType(ItemStack stack) {
        return ItemUtil.getElementType(plugin, stack);
    }

    // Use handling: allow element items to process their own use
    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        // Let ItemManager delegate to specific element items
        Player p = e.getPlayer();
        ItemStack inHand = e.getItem();

        if (inHand != null && isElementItem(inHand)) {
            // Element items handle their own interactions
            itemManager.handleUse(e);
        }
    }

    // Prevent manual drops for element items
    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        ItemStack stack = e.getItemDrop().getItemStack();
        if (isElementItem(stack)) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You cannot drop this item.");
        }
    }

    // Prevent storing in containers
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack cursor = e.getCursor();
        ItemStack current = e.getCurrentItem();
        Inventory top = e.getView().getTopInventory();
        boolean isContainer = top != null && top.getType() != InventoryType.CRAFTING && top.getType() != InventoryType.PLAYER;
        if (!isContainer) return;
        if ((cursor != null && isElementItem(cursor)) || (current != null && isElementItem(current))) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You cannot store this item.");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack item = e.getOldCursor();
        Inventory top = e.getView().getTopInventory();
        boolean isContainer = top != null && top.getType() != InventoryType.CRAFTING && top.getType() != InventoryType.PLAYER;
        if (isContainer && item != null && isElementItem(item)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You cannot store this item.");
        }
    }

    // On pickup, swap element to the item's element and clear old effects
    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack stack = e.getItem().getItemStack();
        if (!isElementItem(stack)) return;
        ElementType type = getElementType(stack);
        if (type == null) return;

        PlayerData pd = elements.data(p.getUniqueId());
        ElementType oldElement = pd.getCurrentElement();

        // Only swap and clear effects if changing to a different element
        if (oldElement != type) {
            // Use ElementManager's setElement which handles effect clearing
            elements.setElement(p, type);
        }
    }

    // On projectile hit damage, apply element item debuffs
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        itemManager.handleDamage(e);
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent e) {
        itemManager.handleLaunch(e);
    }
}