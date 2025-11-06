package hs.elementPlugin.elements.impl.frost.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class FrostAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public FrostAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.FROST) return;

        if (player.isSneaking()) {
            // Ability 2: Frozen Punch
            if (cooldownManager.isOnCooldown(player, "frost_frozen_punch")) {
                player.sendMessage("§cFrozen Punch is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Frozen Punch ability
            cooldownManager.setCooldown(player, "frost_frozen_punch", 20); // 1 second cooldown
            event.setCancelled(true);
        } else {
            // Ability 1: Freezing Circle
            if (cooldownManager.isOnCooldown(player, "frost_freezing_circle")) {
                player.sendMessage("§cFreezing Circle is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Freezing Circle ability
            cooldownManager.setCooldown(player, "frost_freezing_circle", 15); // 0.75 second cooldown
            event.setCancelled(true);
        }
    }
}