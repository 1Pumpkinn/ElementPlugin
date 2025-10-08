package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.gui.ElementSelectionGUI;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class GUIListener implements Listener {
    private final ElementPlugin plugin;

    public GUIListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (title.contains("Select Your Element")) {
            event.setCancelled(true);
            
            ElementSelectionGUI gui = ElementSelectionGUI.getGUI(player.getUniqueId());
            if (gui != null) {
                gui.handleClick(event.getRawSlot());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        
        String title = event.getView().getTitle();
        if (title.contains("Select Your Element")) {
            ElementSelectionGUI.removeGUI(player.getUniqueId());
        }
    }
    
    @EventHandler
    public void onRerollerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null) return;
        
        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, ItemKeys.KEY_REROLLER), PersistentDataType.BYTE)) {
            event.setCancelled(true);
            
            // Check if player is already rolling
            if (plugin.getElementManager().isCurrentlyRolling(player)) {
                player.sendMessage(ChatColor.RED + "You are already rerolling your element!");
                return;
            }
            
            // Remove one reroller item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }
            
            // Automatically reroll the element instead of opening GUI
            plugin.getElementManager().rollAndAssign(player);
            player.sendMessage(ChatColor.GREEN + "Your element has been rerolled!");
        }
    }
    
    @EventHandler
    public void onElementItemUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        if (item == null || !item.hasItemMeta()) return;
        
        // Check if this is an element item
        if (item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE)) {
            
            // Only handle right-click actions
            if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && 
                event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            
            event.setCancelled(true);
            
            // Get the element type from the item
            String elementTypeString = item.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING);
            
            if (elementTypeString == null) {
                player.sendMessage(ChatColor.RED + "Invalid element item!");
                return;
            }
            
            try {
                hs.elementPlugin.elements.ElementType elementType = 
                    hs.elementPlugin.elements.ElementType.valueOf(elementTypeString);
                
                // Check if player already has this element
                hs.elementPlugin.data.PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
                if (pd.getCurrentElement() == elementType) {
                    player.sendMessage(ChatColor.YELLOW + "You are already attuned to " + elementType.name() + "!");
                    return;
                }
                
                // Apply the element
                plugin.getElementManager().assignElement(player, elementType);
                player.sendMessage(ChatColor.GREEN + "You have attuned to " + ChatColor.AQUA + elementType.name() + ChatColor.GREEN + "!");
                
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Invalid element type!");
            }
        }
    }
}
