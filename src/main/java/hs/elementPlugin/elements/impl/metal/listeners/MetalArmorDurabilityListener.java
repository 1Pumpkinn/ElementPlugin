package hs.elementPlugin.elements.impl.metal.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;

/**
 * Metal Upside 2: Armor breaks slower (50% reduced durability loss)
 * Requires Upgrade II
 */
public class MetalArmorDurabilityListener implements Listener {
    private final ElementManager elementManager;

    public MetalArmorDurabilityListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();

        // Check if player has Metal element with Upgrade II
        var pd = elementManager.data(player.getUniqueId());
        if (pd.getCurrentElement() != ElementType.METAL || pd.getUpgradeLevel(ElementType.METAL) < 2) {
            return;
        }

        // Check if the damaged item is armor
        if (!isArmor(event.getItem().getType())) {
            return;
        }

        // 30% chance to trigger armor protection
        if (Math.random() < 0.3) {
            // When triggered, 50% chance to reduce damage by half
            if (Math.random() < 0.5) {
                // Reduce damage by 50% (half durability loss)
                int originalDamage = event.getDamage();
                int reducedDamage = Math.max(1, originalDamage / 2);
                event.setDamage(reducedDamage);
            }
        }
    }

    private boolean isArmor(org.bukkit.Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") ||
                name.endsWith("_BOOTS");
    }
}