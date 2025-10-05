package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.gui.ElementSelectionGUI;
import hs.elementPlugin.items.ItemKeys;
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
            
            // Remove one reroller item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }
            
            // Open element selection GUI
            ElementSelectionGUI gui = new ElementSelectionGUI(plugin, player, true);
            gui.open();
        }
    }
}