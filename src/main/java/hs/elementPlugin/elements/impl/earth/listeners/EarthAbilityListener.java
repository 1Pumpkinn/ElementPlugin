package hs.elementPlugin.elements.impl.earth.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class EarthAbilityListener implements Listener {
    private final ElementManager elementManager;

    public EarthAbilityListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.METAL) return;

        // Cancel the event to prevent hand swapping
        event.setCancelled(true);

        if (player.isSneaking()) {
            // Ability 2:
            elementManager.useAbility2(player);
        } else {
            // Ability 1:
            elementManager.useAbility1(player);
        }
    }
}
