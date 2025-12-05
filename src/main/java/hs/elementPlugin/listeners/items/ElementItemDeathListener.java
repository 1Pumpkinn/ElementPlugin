package hs.elementPlugin.listeners.items;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

public record ElementItemDeathListener(ElementPlugin plugin, ElementManager elements) implements Listener {

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

        // For advanced elements (Life/Death/Metal/Frost): reroll to basic element
        if (shouldRerollOnDeath(currentElement)) {
            plugin.getLogger().info("Player " + e.getEntity().getName() +
                    " died with advanced element " + currentElement + " - rerolling to basic element");

            // Schedule element reroll for after respawn to avoid death loop
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (e.getEntity().isOnline()) {
                        // Reroll to a basic element (AIR, WATER, FIRE, EARTH)
                        elements.assignRandomDifferentElement(e.getEntity());
                        e.getEntity().sendMessage(ChatColor.YELLOW +
                                "You lost your advanced element and rolled a new basic element!");
                    }
                }
            }.runTaskLater(plugin, 40L); // Wait 2 seconds after death (respawn time)
        }
    }

    // Advanced elements (Life, Death, Metal, Frost) cause reroll on death
    private boolean shouldRerollOnDeath(ElementType t) {
        return t == ElementType.LIFE || t == ElementType.DEATH ||
                t == ElementType.METAL || t == ElementType.FROST;
    }
}