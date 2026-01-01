package saturn.elementPlugin.listeners.items;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.items.ItemKeys;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class UpgraderListener implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public UpgraderListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onUpgraderUse(PlayerInteractEvent event) {

        /* =========================
           Interaction filter
           ========================= */
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        /* =========================
           Item validation
           ========================= */
        if (item == null ||
                (item.getType() != Material.AMETHYST_SHARD &&
                        item.getType() != Material.ECHO_SHARD) ||
                !item.hasItemMeta()) {
            return;
        }

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey upgraderKey = ItemKeys.upgraderLevel(plugin);

        if (!pdc.has(upgraderKey, PersistentDataType.INTEGER)) {
            return;
        }

        /* =========================
           Cancel vanilla behaviour
           ========================= */
        event.setCancelled(true);

        /* =========================
           Element validation
           ========================= */
        var playerData = elementManager.data(player.getUniqueId());
        var currentElement = playerData.getCurrentElement();

        if (currentElement == null) {
            player.sendMessage(ChatColor.RED + "You don't have an element yet!");
            player.sendMessage(ChatColor.YELLOW + "Use a " +
                    ChatColor.LIGHT_PURPLE + "Reroller" +
                    ChatColor.YELLOW + " to obtain one first.");
            return;
        }

        int upgraderLevel = pdc.get(upgraderKey, PersistentDataType.INTEGER);
        int currentUpgradeLevel = playerData.getUpgradeLevel(currentElement);

        /* =========================
           Upgrade I
           ========================= */
        if (upgraderLevel == 1) {

            if (currentUpgradeLevel >= 1) {
                player.sendMessage(ChatColor.RED + "You already have Upgrade I.");
                return;
            }

            playerData.setUpgradeLevel(currentElement, 1);
            plugin.getDataStore().save(playerData);
            elementManager.applyUpsides(player);

            consumeItem(player);

            player.sendMessage(ChatColor.GREEN + "You have unlocked " +
                    ChatColor.GOLD + "Upgrade I");
        }

        /* =========================
           Upgrade II
           ========================= */
        else if (upgraderLevel == 2) {

            if (currentUpgradeLevel < 1) {
                player.sendMessage(ChatColor.RED +
                        "You need Upgrade I before using Upgrade II!");
                return;
            }

            if (currentUpgradeLevel >= 2) {
                player.sendMessage(ChatColor.RED + "You already have Upgrade II.");
                return;
            }

            playerData.setUpgradeLevel(currentElement, 2);
            plugin.getDataStore().save(playerData);
            elementManager.applyUpsides(player);

            consumeItem(player);

            player.sendMessage(ChatColor.GREEN + "You have unlocked " +
                    ChatColor.GOLD + "Upgrade II");
        }
    }

    /* =========================
       Utility
       ========================= */
    private void consumeItem(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
    }
}
