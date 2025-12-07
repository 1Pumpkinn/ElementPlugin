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
        if (!plugin.getConfigManager().isUpgradersDropOnDeath()) {
            return;
        }

        PlayerData pd = elements.data(e.getEntity().getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement != null) {
            int currentLevel = pd.getUpgradeLevel(currentElement);

            if (currentLevel > 0) {
                for (int i = 0; i < currentLevel; i++) {
                    if (i == 0) {
                        e.getDrops().add(plugin.getItemManager().createUpgrader1());
                    } else {
                        e.getDrops().add(plugin.getItemManager().createUpgrader2());
                    }
                }

                pd.setUpgradeLevel(currentElement, 0);
                plugin.getDataStore().save(pd);

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