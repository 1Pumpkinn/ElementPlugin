package hs.elementPlugin.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public final class CoreConsumptionHandler {
    private CoreConsumptionHandler() {}

    public static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    /**
     * Handles consuming Life/Death core items on right-click. Returns true if the event was handled.
     */
    public static boolean handleCoreConsume(PlayerInteractEvent e, ElementPlugin plugin, ElementManager elements) {
        Player player = e.getPlayer();
        ItemStack inHand = e.getItem();
        if (inHand == null || !ItemUtil.isElementItem(plugin, inHand)) return false;

        ElementType type = ItemUtil.getElementType(plugin, inHand);
        if (type != ElementType.LIFE && type != ElementType.DEATH) return false;

        if (!isRightClick(e.getAction())) return false;

        PlayerData pd = elements.data(player.getUniqueId());

        // Switch to the core's element
        elements.setElement(player, type);

        // Mark that they have consumed this core
        pd.addElementItem(type);
        plugin.getDataStore().save(pd);

        // Consume the item - use the actual item in hand (main or off-hand)
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        if (itemInHand.equals(inHand)) {
            itemInHand.setAmount(itemInHand.getAmount() - 1);
        } else {
            itemInHand = player.getInventory().getItemInOffHand();
            if (itemInHand.equals(inHand)) {
                itemInHand.setAmount(itemInHand.getAmount() - 1);
            }
        }

        player.sendMessage(ChatColor.GREEN + "You consumed the " +
                hs.elementPlugin.items.ElementCoreItem.getDisplayName(type) + ChatColor.GREEN + "!");

        e.setCancelled(true);
        return true;
    }
}


