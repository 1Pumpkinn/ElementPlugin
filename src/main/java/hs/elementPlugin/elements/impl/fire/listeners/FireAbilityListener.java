package hs.elementPlugin.elements.impl.fire.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class FireAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public FireAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.FIRE) return;

        if (player.isSneaking()) {
            // Ability 2: Fire Summon
            if (cooldownManager.isOnCooldown(player, "fire_summon")) {
                player.sendMessage("§cFire Summon is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Fire Summon ability
            cooldownManager.setCooldown(player, "fire_summon", 30); // 1.5 second cooldown
            event.setCancelled(true);
        } else {
            // Ability 1: Fire Breath
            if (cooldownManager.isOnCooldown(player, "fire_breath")) {
                player.sendMessage("§cFire Breath is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Fire Breath ability
            cooldownManager.setCooldown(player, "fire_breath", 12); // 0.6 second cooldown
            event.setCancelled(true);
        }
    }
}
