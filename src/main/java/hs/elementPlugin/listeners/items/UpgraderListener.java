package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class UpgraderListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public UpgraderListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onUpgraderUse(PlayerInteractEvent event) {
        // Only handle right-click air or right-click block
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Check if the item is valid (either Upgrader 1 or Upgrader 2)
        if (item == null || (item.getType() != Material.AMETHYST_SHARD && item.getType() != Material.NETHER_STAR)) {
            return;
        }

        // Check if the item has metadata
        if (!item.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey upgraderKey = ItemKeys.upgraderLevel(plugin);

        // Check if this is an upgrader item
        if (!pdc.has(upgraderKey, PersistentDataType.INTEGER)) {
            return;
        }

        // Get the upgrader level
        int upgraderLevel = pdc.get(upgraderKey, PersistentDataType.INTEGER);
        
        // Get player's current element and upgrade level
        var playerData = elementManager.data(player.getUniqueId());
        var currentElement = playerData.getCurrentElement();
        int currentUpgradeLevel = playerData.getUpgradeLevel(currentElement);

        // Cancel the event to prevent normal item use
        event.setCancelled(true);

        // Handle Upgrader 1
        if (upgraderLevel == 1) {
            if (currentUpgradeLevel >= 1) {
                player.sendMessage(ChatColor.RED + "You already have Upgrade I for your " + 
                    ChatColor.YELLOW + currentElement.toString() + ChatColor.RED + " element!");
                return;
            }
            
            // Apply the upgrade
            playerData.setUpgradeLevel(currentElement, 1);
            plugin.getDataStore().save(playerData);
            
            // Reapply upsides to include new upgrade benefits
            elementManager.applyUpsides(player);
            
            // Remove one item from hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            player.sendMessage(ChatColor.GREEN + "You have unlocked " + ChatColor.GOLD + "Upgrade I" + 
                ChatColor.GREEN + " for your " + ChatColor.YELLOW + currentElement.toString() + 
                ChatColor.GREEN + " element!");
        }
        // Handle Upgrader 2
        else if (upgraderLevel == 2) {
            if (currentUpgradeLevel < 1) {
                player.sendMessage(ChatColor.RED + "You need Upgrade I before you can use Upgrade II!");
                return;
            }
            
            if (currentUpgradeLevel >= 2) {
                player.sendMessage(ChatColor.RED + "You already have Upgrade II for your " + 
                    ChatColor.YELLOW + currentElement.toString() + ChatColor.RED + " element!");
                return;
            }
            
            // Apply the upgrade
            playerData.setUpgradeLevel(currentElement, 2);
            plugin.getDataStore().save(playerData);
            
            // Reapply upsides to include new upgrade benefits (including Upside 2)
            elementManager.applyUpsides(player);
            
            // Remove one item from hand
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().setItemInMainHand(null);
            }
            
            player.sendMessage(ChatColor.GREEN + "You have unlocked " + ChatColor.GOLD + "Upgrade II" + 
                ChatColor.GREEN + " for your " + ChatColor.YELLOW + currentElement.toString() + 
                ChatColor.GREEN + " element!");
        }
    }
}
