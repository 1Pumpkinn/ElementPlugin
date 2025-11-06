package hs.elementPlugin.elements.impl.water.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class WaterAbilityListener implements Listener {
    private final ElementManager elementManager;

    public WaterAbilityListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.WATER) return;

        // Cancel the event to prevent hand swapping
        event.setCancelled(true);

        if (player.isSneaking()) {
            // Ability 2: Water Geyser
            elementManager.useAbility2(player);
        } else {
            // Ability 1: Water Beam
            elementManager.useAbility1(player);
        }
    }
}