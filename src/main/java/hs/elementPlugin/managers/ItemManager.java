package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.api.ElementItem;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

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
}