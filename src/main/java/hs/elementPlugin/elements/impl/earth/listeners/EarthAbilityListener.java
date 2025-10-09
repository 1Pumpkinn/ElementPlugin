package hs.elementPlugin.elements.impl.earth.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class EarthAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public EarthAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.EARTH) return;

        if (player.isSneaking()) {
            // Ability 2: Earth Tunnel
            if (cooldownManager.isOnCooldown(player, "earth_tunnel")) {
                player.sendMessage("§cEarth Tunnel is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Earth Tunnel ability
            cooldownManager.setCooldown(player, "earth_tunnel", 25); // 1.25 second cooldown
            event.setCancelled(true);
        } else {
            // Ability 1: Earth Charm
            if (cooldownManager.isOnCooldown(player, "earth_charm")) {
                player.sendMessage("§cEarth Charm is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Earth Charm ability
            cooldownManager.setCooldown(player, "earth_charm", 18); // 0.9 second cooldown
            event.setCancelled(true);
        }
    }
}
