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

public class DeathListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;

    public DeathListener(ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

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
                
                e.getEntity().sendMessage(ChatColor.RED + "Your upgraders dropped upon death!");
            }
        }
        // Drop core for Life/Death elements if player holds it
        if (currentElement == ElementType.LIFE || currentElement == ElementType.DEATH) {
            Iterator<ItemStack> iter = e.getEntity().getInventory().iterator();
            while (iter.hasNext()) {
                ItemStack item = iter.next();
                if (ItemUtil.isElementItem(plugin, item)
                    && ItemUtil.getElementType(plugin, item) == currentElement) {
                    e.getDrops().add(item.clone());
                    iter.remove(); // Remove from player's inventory
                    break;
                }
            }
            // Reroll a new element for the player
            elements.assignRandomWithTitle(e.getEntity());
            e.getEntity().sendMessage(ChatColor.YELLOW + "Your core dropped and you have been attuned to a new element!");
        }
    }
}