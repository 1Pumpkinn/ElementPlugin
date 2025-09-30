package hs.elementPlugin.items.api;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;

public interface ElementItem {
    ElementType getElementType();

    ItemStack create(ElementPlugin plugin);

    void registerRecipe(ElementPlugin plugin);

    boolean isItem(ItemStack stack, ElementPlugin plugin);

    boolean handleUse(PlayerInteractEvent e, ElementPlugin plugin, ManaManager mana, ConfigManager config);

    void handleDamage(EntityDamageByEntityEvent e, ElementPlugin plugin);

    default void handleLaunch(ProjectileLaunchEvent e, ElementPlugin plugin, ManaManager mana, ConfigManager config) {}
}
