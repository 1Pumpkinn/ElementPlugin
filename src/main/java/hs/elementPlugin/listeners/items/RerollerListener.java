package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.util.SmartEffectCleaner;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
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

        if (item == null || !item.hasItemMeta()) return;

        if (!item.getItemMeta().getPersistentDataContainer()
                .has(ItemKeys.reroller(plugin), PersistentDataType.BYTE)) return;

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        if (event.getHand() != EquipmentSlot.HAND) return;
        event.setCancelled(true);

        // Ignore if already rolling
        if (plugin.getElementManager().isCurrentlyRolling(player)) return;
        SmartEffectCleaner.clearForElementChange(plugin, player);

        plugin.getLogger().info(
                "Reroller used by " + player.getName() + " - cleared all infinite element effects"
        );

        // Consume one reroller
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().removeItem(item);
        }

        // Roll to a DIFFERENT basic element
        plugin.getElementManager().rollAndAssignDifferent(player);

        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1.0f, 1.2f);
        player.sendMessage(
                Component.text("Your element has been rerolled!")
                        .color(NamedTextColor.GREEN)
        );
    }
}
