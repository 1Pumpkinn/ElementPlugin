package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public record DeathListener(ElementPlugin plugin, ElementManager elements) implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
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
                // Schedule this for next tick to avoid issues during death event
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (e.getEntity().isOnline()) {
                            elements.applyUpsides(e.getEntity());
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }
    }
}