package hs.elementPlugin.elements.impl.life.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class LifeAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public LifeAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.LIFE) return;

        if (player.isSneaking()) {
            // Ability 2: Life Regen
            if (cooldownManager.isOnCooldown(player, "life_regen")) {
                player.sendMessage("§cLife Regen is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Life Regen ability
            cooldownManager.setCooldown(player, "life_regen", 20); // 1 second cooldown
            event.setCancelled(true);
        } else {
            // Ability 1: Life Healing Beam
            if (cooldownManager.isOnCooldown(player, "life_healing_beam")) {
                player.sendMessage("§cLife Healing Beam is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Life Healing Beam ability
            cooldownManager.setCooldown(player, "life_healing_beam", 15); // 0.75 second cooldown
            event.setCancelled(true);
        }
    }
}
