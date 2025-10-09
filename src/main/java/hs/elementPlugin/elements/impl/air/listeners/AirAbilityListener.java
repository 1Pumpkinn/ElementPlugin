package hs.elementPlugin.elements.impl.air.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class AirAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public AirAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.AIR) return;

        if (player.isSneaking()) {
            // Ability 2: Air Dash
            if (cooldownManager.isOnCooldown(player, "air_dash")) {
                player.sendMessage("§cAir Dash is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Air Dash ability
            cooldownManager.setCooldown(player, "air_dash", 20); // 1 second cooldown
            event.setCancelled(true);
        } else {
            // Ability 1: Air Blast
            if (cooldownManager.isOnCooldown(player, "air_blast")) {
                player.sendMessage("§cAir Blast is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Air Blast ability
            cooldownManager.setCooldown(player, "air_blast", 8); // 0.4 second cooldown
            event.setCancelled(true);
        }
    }
}
