package hs.elementPlugin.elements.impl.metal.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class MetalAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public MetalAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.METAL) return;

        if (player.isSneaking()) {
            // Ability 2: Metal Dash
            if (cooldownManager.isOnCooldown(player, "metal_dash")) {
                player.sendMessage("§cMetal Dash is on cooldown!");
                event.setCancelled(true);
                return;
            }
            cooldownManager.setCooldown(player, "metal_dash", 15);
            event.setCancelled(true);
        } else {
            // Ability 1: Chain Reel
            if (cooldownManager.isOnCooldown(player, "metal_chain")) {
                player.sendMessage("§cChain Reel is on cooldown!");
                event.setCancelled(true);
                return;
            }
            cooldownManager.setCooldown(player, "metal_chain", 10);
            event.setCancelled(true);
        }
    }
}