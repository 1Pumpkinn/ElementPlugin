package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.util.SmartEffectCleaner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Handles basic reroller usage
 * Rerolls to basic elements: AIR, WATER, FIRE, EARTH
 * Clears ALL infinite element effects (including advanced element effects)
 */
public class RerollerListener implements Listener {
    private final ElementPlugin plugin;

    public RerollerListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onRerollerUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        if (item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer()
                .has(ItemKeys.reroller(plugin), PersistentDataType.BYTE)) {

            org.bukkit.event.block.Action action = event.getAction();
            if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
                    action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            // CRITICAL FIX: Prevent offhand usage
            if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) {
                return;
            }

            // CRITICAL FIX: Always cancel the event to prevent spam
            event.setCancelled(true);

            // Check if player is already rolling
            if (plugin.getElementManager().isCurrentlyRolling(player)) {
                // Don't send message on every spam click - just silently ignore
                return;
            }

            // CRITICAL: Clear ALL infinite element effects (including advanced element effects)
            // This ensures switching from METAL/FROST/LIFE/DEATH to basic elements works correctly
            SmartEffectCleaner.clearForElementChange(plugin, player);

            plugin.getLogger().info("Reroller used by " + player.getName() + " - cleared all infinite element effects");

            // Remove one reroller item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }

            // GUARANTEED DIFFERENT: Automatically reroll to a DIFFERENT basic element
            plugin.getElementManager().rollAndAssignDifferent(player);
            player.sendMessage(net.kyori.adventure.text.Component.text("Your element has been rerolled!").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
        }
    }
}