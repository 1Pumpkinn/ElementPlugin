package hs.elementPlugin.gui;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ElementSelectionGUI {
    private static final String INVENTORY_TITLE = ChatColor.DARK_PURPLE + "Select Your Element";
    private static final Map<UUID, ElementSelectionGUI> openGuis = new HashMap<>();
    
    private final ElementPlugin plugin;
    private final ElementManager elementManager;
    private final Player player;
    private final Inventory inventory;
    private final boolean isReroll;
    
    public ElementSelectionGUI(ElementPlugin plugin, Player player, boolean isReroll) {
        this.plugin = plugin;
        this.elementManager = plugin.getElementManager();
        this.player = player;
        this.isReroll = isReroll;
        this.inventory = Bukkit.createInventory(null, 9, INVENTORY_TITLE);
        
        setupItems();
        openGuis.put(player.getUniqueId(), this);
    }

    private void setupItems() {
        // Fire element item
        ItemStack fireItem = new ItemStack(Material.FIRE_CHARGE);
        ItemMeta fireMeta = fireItem.getItemMeta();
        fireMeta.setDisplayName(ChatColor.RED + "Fire Element");

        fireItem.setItemMeta(fireMeta);

        // Water element item
        ItemStack waterItem = new ItemStack(Material.WATER_BUCKET);
        ItemMeta waterMeta = waterItem.getItemMeta();
        waterMeta.setDisplayName(ChatColor.BLUE + "Water Element");

        waterItem.setItemMeta(waterMeta);

        // Earth element item
        ItemStack earthItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta earthMeta = earthItem.getItemMeta();
        earthMeta.setDisplayName(ChatColor.GREEN + "Earth Element");

        earthItem.setItemMeta(earthMeta);

        // Air element item
        ItemStack airItem = new ItemStack(Material.FEATHER);
        ItemMeta airMeta = airItem.getItemMeta();
        airMeta.setDisplayName(ChatColor.WHITE + "Air Element");

        airItem.setItemMeta(airMeta);


        // Place items in inventory
        inventory.setItem(0, fireItem);
        inventory.setItem(2, waterItem);
        inventory.setItem(4, earthItem);
        inventory.setItem(6, airItem);
    }
    
    public void open() {
        player.openInventory(inventory);
    }

    public void handleClick(int slot) {
        ElementType selectedElement = null;

        switch (slot) {
            case 0:
                selectedElement = ElementType.FIRE;
                break;
            case 2:
                selectedElement = ElementType.WATER;
                break;
            case 4:
                selectedElement = ElementType.EARTH;
                break;
            case 6:
                selectedElement = ElementType.AIR;
                break;
        }
        
        if (selectedElement != null) {
            player.closeInventory();
            
            if (isReroll) {
                elementManager.setElement(player, selectedElement);
                player.sendMessage(ChatColor.GREEN + "Your element has been changed to " + 
                    ChatColor.GOLD + selectedElement.name());
            } else {
                elementManager.assignElement(player, selectedElement);
                player.sendMessage(ChatColor.GREEN + "You have selected the " + 
                    ChatColor.GOLD + selectedElement.name() + ChatColor.GREEN + " element!");
            }
        }
    }


    
    public static ElementSelectionGUI getGUI(UUID playerId) {
        return openGuis.get(playerId);
    }
    
    public static void removeGUI(UUID playerId) {
        openGuis.remove(playerId);
    }
}