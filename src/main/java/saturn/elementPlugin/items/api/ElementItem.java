package saturn.elementPlugin.items.api;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ManaManager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public interface ElementItem {
    ElementType getElementType();

    ItemStack create(ElementPlugin plugin);

    void registerRecipe(ElementPlugin plugin);

    boolean isItem(ItemStack stack, ElementPlugin plugin);

    boolean handleUse(PlayerInteractEvent e, ElementPlugin plugin, ManaManager mana);

    void handleDamage(EntityDamageByEntityEvent e, ElementPlugin plugin);

    default void handleLaunch(ProjectileLaunchEvent e, ElementPlugin plugin, ManaManager mana) {}
}