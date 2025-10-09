package hs.elementPlugin.elements.impl.water.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class WaterAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public WaterAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.WATER) return;

        if (player.isSneaking()) {
            // Ability 2: Water Geyser
            if (cooldownManager.isOnCooldown(player, "water_geyser")) {
                player.sendMessage("§cWater Geyser is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Water Geyser ability
            cooldownManager.setCooldown(player, "water_geyser", 15); // 0.75 second cooldown
            event.setCancelled(true);
        } else {
            // Ability 1: Water Beam
            if (cooldownManager.isOnCooldown(player, "water_beam")) {
                player.sendMessage("§cWater Beam is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Water Beam ability
            cooldownManager.setCooldown(player, "water_beam", 10); // 0.5 second cooldown
            event.setCancelled(true);
        }
    }
}
