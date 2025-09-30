package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.api.ElementItem;
import hs.elementPlugin.util.ItemUtil;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.EnumMap;
import java.util.Map;

public class ItemManager {
    private final ElementPlugin plugin;
    private final ManaManager mana;
    private final Map<ElementType, ElementItem> items = new EnumMap<>(ElementType.class);

    public ItemManager(ElementPlugin plugin, ManaManager mana) {
        this.plugin = plugin;
        this.mana = mana;
    }

    public void register(ElementItem item) {
        items.put(item.getElementType(), item);
        item.registerRecipe(plugin);
    }

    public boolean handleUse(PlayerInteractEvent e) {
        var p = e.getPlayer();
        var main = p.getInventory().getItemInMainHand();
        var off = p.getInventory().getItemInOffHand();
        for (ElementItem item : items.values()) {
            if (item.isItem(main, plugin) || item.isItem(off, plugin)) {
                boolean handled = item.handleUse(e, plugin, mana);
                if (handled) return true;
            }
        }
        return false;
    }

    public void handleDamage(EntityDamageByEntityEvent e) {
        for (ElementItem item : items.values()) {
            item.handleDamage(e, plugin);
        }
    }

    public void handleLaunch(ProjectileLaunchEvent e) {
        for (ElementItem item : items.values()) {
            item.handleLaunch(e, plugin, mana);
        }
    }

    public boolean isElementItem(org.bukkit.inventory.ItemStack stack) {
        return ItemUtil.isElementItem(plugin, stack);
    }

    public ElementType getElementType(org.bukkit.inventory.ItemStack stack) {
        return ItemUtil.getElementType(plugin, stack);
    }
}