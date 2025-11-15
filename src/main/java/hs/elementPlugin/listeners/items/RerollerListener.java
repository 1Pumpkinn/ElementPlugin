package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.*;
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
            // Only activate on right-click, not left-click/hit or other actions
            org.bukkit.event.block.Action action = event.getAction();
            if (action != org.bukkit.event.block.Action.RIGHT_CLICK_AIR &&
                    action != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            // CRITICAL: Check if clicking on a pedestal - if so, don't use the reroller
            if (action == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) {
                org.bukkit.block.Block clickedBlock = event.getClickedBlock();
                if (clickedBlock != null && clickedBlock.getType() == org.bukkit.Material.LODESTONE) {
                    // Check if it's a custom block (pedestal) using BlockDataStorage
                    hs.elementSmpUtility.storage.BlockDataStorage blockStorage =
                            ((hs.elementPlugin.ElementPlugin) plugin).getBlockStorage();
                    String blockId = blockStorage.getCustomBlockIdCached(clickedBlock.getLocation());

                    if ("pedestal".equals(blockId)) {
                        // This is a pedestal - don't use the reroller here
                        // The PedestalInteractionListener will handle placing it on the pedestal
                        return;
                    }
                }
            }

            event.setCancelled(true);

            // Check if player is already rolling
            if (plugin.getElementManager().isCurrentlyRolling(player)) {
                player.sendMessage(net.kyori.adventure.text.Component.text("You are already rerolling your element!").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                return;
            }

            // CRITICAL FIX: Clear old element effects BEFORE rerolling
            PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
            ElementType oldElement = pd.getCurrentElement();
            if (oldElement != null) {
                clearOldElementEffects(player, oldElement);
            }

            // Remove one reroller item
            if (item.getAmount() > 1) {
                item.setAmount(item.getAmount() - 1);
            } else {
                player.getInventory().removeItem(item);
            }

            // Automatically reroll the element instead of opening GUI
            plugin.getElementManager().rollAndAssign(player);
            player.sendMessage(net.kyori.adventure.text.Component.text("Your element has been rerolled!").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
        }
    }

    /**
     * Clear old element effects properly
     */
    private void clearOldElementEffects(Player player, ElementType oldElement) {
        if (oldElement == null) return;

        // Use the Element's clearEffects method
        var element = plugin.getElementManager().get(oldElement);
        if (element != null) {
            element.clearEffects(player);
        }

        // Special handling for Life element - reset max health
        if (oldElement == ElementType.LIFE) {
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) {
                attr.setBaseValue(20.0);
                if (!player.isDead() && player.getHealth() > 0 && player.getHealth() > 20.0) {
                    player.setHealth(20.0);
                }
            }
        }
    }
}