package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.attribute.Attribute;
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

            // CRITICAL FIX: Clear ALL element effects BEFORE starting reroll
            PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
            ElementType oldElement = pd.getCurrentElement();

            // Clear effects from ALL elements to prevent stacking
            clearAllElementEffects(player);

            // Remove one reroller item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }

            // Automatically reroll the element
            plugin.getElementManager().rollAndAssign(player);
            player.sendMessage(net.kyori.adventure.text.Component.text("Your element has been rerolled!").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
        }
    }

    /**
     * CRITICAL: Clear ALL element effects to prevent stacking
     * This fixes the bug where logging out during reroll causes effect accumulation
     */
    private void clearAllElementEffects(Player player) {
        // Clear effects from EVERY element type
        for (ElementType type : ElementType.values()) {
            Element element = plugin.getElementManager().get(type);
            if (element != null) {
                element.clearEffects(player);
            }
        }

        // Reset max health to default (20 HP) while preserving current health
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null && attr.getBaseValue() != 20.0) {
            // Store current health before changing max health
            double currentHealth = player.getHealth();
            attr.setBaseValue(20.0);

            // Restore current health (capped at new max if necessary)
            if (!player.isDead() && currentHealth > 0) {
                player.setHealth(Math.min(currentHealth, 20.0));
            }
        }

        plugin.getLogger().fine("Cleared all element effects for " + player.getName() + " before reroll");
    }
}