package hs.elementPlugin.elements.impl.death.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public class DeathAbilityListener implements Listener {
    private final ElementManager elementManager;
    private final CooldownManager cooldownManager;

    public DeathAbilityListener(ElementManager elementManager, CooldownManager cooldownManager) {
        this.elementManager = elementManager;
        this.cooldownManager = cooldownManager;
    }

    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) != ElementType.DEATH) return;

        if (player.isSneaking()) {
            // Ability 2: Death Summon Undead
            if (cooldownManager.isOnCooldown(player, "death_summon_undead")) {
                player.sendMessage("§cDeath Summon Undead is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Death Summon Undead ability
            cooldownManager.setCooldown(player, "death_summon_undead", 40); // 2 second cooldown
            event.setCancelled(true);
        } else {
            // Ability 1: Death Wither Skull
            if (cooldownManager.isOnCooldown(player, "death_wither_skull")) {
                player.sendMessage("§cDeath Wither Skull is on cooldown!");
                event.setCancelled(true);
                return;
            }
            // Execute Death Wither Skull ability
            cooldownManager.setCooldown(player, "death_wither_skull", 25); // 1.25 second cooldown
            event.setCancelled(true);
        }
    }
}
