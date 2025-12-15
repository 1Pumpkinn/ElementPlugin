package saturn.elementPlugin.listeners.items;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public record DeathListener(ElementPlugin plugin, ElementManager elements) implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!plugin.isUpgradersDropOnDeath()) {
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