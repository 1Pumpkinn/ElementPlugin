package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.api.ElementItem;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

public class ItemManager {
    private final ElementPlugin plugin;
    private final ManaManager mana;
    private final ConfigManager configManager;
    private final Map<ElementType, ElementItem> items = new EnumMap<>(ElementType.class);

    public ItemManager(ElementPlugin plugin, ManaManager mana, ConfigManager configManager) {
        this.plugin = plugin;
        this.mana = mana;
        this.configManager = configManager;
    }

    public void register(ElementItem item) {
        items.put(item.getElementType(), item);
        item.registerRecipe(plugin);
    }

    public void handleUse(PlayerInteractEvent e) {
        for (ElementItem item : items.values()) {
            if (item.handleUse(e, plugin, mana, configManager)) {
                // If an item handled the event, stop processing
                return;
            }
        }
    }

    public void handleDamage(EntityDamageByEntityEvent e) {
        for (ElementItem item : items.values()) {
            item.handleDamage(e, plugin);
        }
    }

    public void handleLaunch(ProjectileLaunchEvent e) {
        for (ElementItem item : items.values()) {
            item.handleLaunch(e, plugin, mana, configManager);
        }
    }
    
    /**
     * Creates an Upgrader1 item
     * @return The created ItemStack
     */
    public ItemStack createUpgrader1() {
        return hs.elementPlugin.items.Upgrader1Item.make(plugin);
    }
    
    /**
     * Creates an Upgrader2 item
     * @return The created ItemStack
     */
    public ItemStack createUpgrader2() {
        return hs.elementPlugin.items.Upgrader2Item.make(plugin);
    }
}