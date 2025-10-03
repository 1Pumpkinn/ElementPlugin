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
        Action a = e.getAction();

        // Handle ability 1: Shift + Left Click (main hand only)
        if (e.getHand() == EquipmentSlot.HAND && e.getPlayer().isSneaking() && 
            (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK)) {
            boolean ok = elements.useAbility1(e.getPlayer());
            if (ok) e.setCancelled(true);
            return;
        }
    }

    // Handle ability 2: Swap hands (F key) - works for all elements including Air
    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        
        // Try to use ability 2 regardless of offhand contents
        // This allows Air element to work even with empty offhand
        boolean ok = elements.useAbility2(player);
        if (ok) {
            e.setCancelled(true); // Prevent the actual item swap
        }
    }
}
