package hs.elementPlugin.listeners;

import hs.elementPlugin.managers.ElementManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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

        // Only handle main-hand to avoid duplicate events
        if (e.getHand() != EquipmentSlot.HAND) return;

        // Require sneaking for both abilities
        if (!e.getPlayer().isSneaking()) return;

        // Ability 1: Shift + Left Click
        if (a == Action.LEFT_CLICK_AIR || a == Action.LEFT_CLICK_BLOCK) {
            boolean ok = elements.useAbility1(e.getPlayer());
            if (ok) e.setCancelled(true);
            return;
        }

        // Ability 2: Shift + Right Click
        if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
            boolean ok = elements.useAbility2(e.getPlayer());
            if (ok) e.setCancelled(true);
        }
    }
}
