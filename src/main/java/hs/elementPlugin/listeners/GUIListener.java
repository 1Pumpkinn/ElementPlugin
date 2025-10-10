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
    // Prevent rapid re-open loops when inventories transition
    private final java.util.Set<java.util.UUID> suppressReopen = new java.util.HashSet<>();

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
            // Capture close reason to avoid reopening during inventory transitions
            org.bukkit.event.inventory.InventoryCloseEvent.Reason reason = event.getReason();
            // Delay the check to the next tick so element assignment can complete
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Skip if we just opened, or if a new inventory is opening/closed by plugin
                if (suppressReopen.contains(player.getUniqueId())) return;
                if (reason == org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW ||
                    reason == org.bukkit.event.inventory.InventoryCloseEvent.Reason.PLUGIN) {
                    return;
                }
                hs.elementPlugin.managers.ElementManager em = plugin.getElementManager();
                if (em.data(player.getUniqueId()).getCurrentElement() == null) {
                player.sendMessage(net.kyori.adventure.text.Component.text("You must choose an element to play!").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                    suppressReopen.add(player.getUniqueId());
                    new hs.elementPlugin.gui.ElementSelectionGUI(plugin, player, false).open();
                    // Remove suppression shortly after to allow future legitimate closes to trigger reopen
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> suppressReopen.remove(player.getUniqueId()), 2L);
                }
            });
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
                player.sendMessage(net.kyori.adventure.text.Component.text("You are already rerolling your element!").color(net.kyori.adventure.text.format.NamedTextColor.RED));
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
            player.sendMessage(net.kyori.adventure.text.Component.text("Your element has been rerolled!").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
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
                player.sendMessage(net.kyori.adventure.text.Component.text("Invalid element item!").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }
            
            try {
                hs.elementPlugin.elements.ElementType elementType = 
                    hs.elementPlugin.elements.ElementType.valueOf(elementTypeString);
                
                // Check if player already has this element
                hs.elementPlugin.data.PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
                if (pd.getCurrentElement() == elementType) {
                    player.sendMessage(
                        net.kyori.adventure.text.Component.text("You already have ")
                            .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW)
                            .append(net.kyori.adventure.text.Component.text(elementType.name(), net.kyori.adventure.text.format.NamedTextColor.GOLD))
                            .append(net.kyori.adventure.text.Component.text(" as your element!", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                    );
                    return;
                }
                
                // Apply the element
                plugin.getElementManager().assignElement(player, elementType);
                
                // Consume the element core
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                // Give a new matching core immediately after user consumes one
                plugin.getElementManager().giveElementItem(player, elementType);
                
                player.sendMessage(
                    net.kyori.adventure.text.Component.text("You have chosen ")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GREEN)
                        .append(net.kyori.adventure.text.Component.text(elementType.name(), net.kyori.adventure.text.format.NamedTextColor.AQUA))
                        .append(net.kyori.adventure.text.Component.text(" as your element!", net.kyori.adventure.text.format.NamedTextColor.GREEN))
                );
                
            } catch (IllegalArgumentException e) {
                player.sendMessage(net.kyori.adventure.text.Component.text("Invalid element type!").color(net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        }
    }
}
