package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.util.ItemUtil;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Iterator;

public record DeathListener(ElementPlugin plugin, ElementManager elements) implements Listener {

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        PlayerData pd = elements.data(e.getEntity().getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement != null) {
            int currentLevel = pd.getUpgradeLevel(currentElement);

            if (currentLevel > 0) {
                // Drop upgraders based on level
                for (int i = 0; i < currentLevel; i++) {
                    if (i == 0) {
                        // Drop Upgrader 1
                        e.getDrops().add(plugin.getItemManager().createUpgrader1());
                    } else {
                        // Drop Upgrader 2
                        e.getDrops().add(plugin.getItemManager().createUpgrader2());
                    }
                }

                // Reset upgrade level to 0
                pd.setUpgradeLevel(currentElement, 0);
                plugin.getDataStore().save(pd);

                // Reapply upsides to remove any upgrade benefits
                elements.applyUpsides(e.getEntity());

            }
        }
        // For life/death elements: reroll player to new element (core drops naturally from inventory)
        if (shouldDropCore(currentElement)) {
            // Remove the element item flag so they don't have it anymore
            pd.removeElementItem(currentElement);
            plugin.getDataStore().save(pd);

            // Reroll a new element for the player
            elements.assignRandomWithTitle(e.getEntity());
            e.getEntity().sendMessage(ChatColor.YELLOW + "Your core dropped and you rolled a new element!");
        }
    }

    // Utility: easily add more elements that drop cores in the future
    private boolean shouldDropCore(ElementType t) {
        return t == ElementType.LIFE || t == ElementType.DEATH;
    }
}