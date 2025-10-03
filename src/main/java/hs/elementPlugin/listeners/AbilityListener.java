package hs.elementPlugin.listeners;

import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

public class AbilityListener implements Listener {
    private final hs.elementPlugin.ElementPlugin plugin;
    private final ElementManager elements;

    public AbilityListener(hs.elementPlugin.ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        // This handler is no longer used for abilities - abilities now use offhand keybind (F key)
    }

    // Handle abilities using the offhand keybind (F key by default)
    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        
        if (player.isSneaking()) {
            // Ability 2: Shift + Offhand keybind (Shift + F)
            boolean ok = elements.useAbility2(player);
            if (ok) {
                e.setCancelled(true); // Prevent the actual item swap
            }
        } else {
            // Ability 1: Offhand keybind (F key)
            boolean ok = elements.useAbility1(player);
            if (ok) {
                e.setCancelled(true); // Prevent the actual item swap
            }
        }
    }
}
