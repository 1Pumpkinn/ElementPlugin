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

            // FIXED: Use SmartEffectCleaner to clear only old element effects
            SmartEffectCleaner.clearForElementChange(plugin, player);

            // Remove one reroller item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }

            // GUARANTEED DIFFERENT: Automatically reroll to a DIFFERENT element
            plugin.getElementManager().rollAndAssignDifferent(player);
            player.sendMessage(net.kyori.adventure.text.Component.text("Your element has been rerolled!").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
        }
    }
}