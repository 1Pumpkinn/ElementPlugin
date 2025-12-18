package saturn.elementPlugin.listeners.items;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import saturn.elementPlugin.util.SmartEffectCleaner;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DeathListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;
    private final Random random = new Random();

    // Basic elements to reroll to (from regular Reroller)
    private static final ElementType[] BASIC_ELEMENTS = {
            ElementType.AIR,
            ElementType.WATER,
            ElementType.FIRE,
            ElementType.EARTH
    };

    // Advanced elements (from Advanced Reroller)
    private static final ElementType[] ADVANCED_ELEMENTS = {
            ElementType.METAL,
            ElementType.FROST,
            ElementType.LIFE,
            ElementType.DEATH
    };

    public DeathListener(ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        PlayerData pd = elements.data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        // Handle upgrader drops (if enabled)
        if (plugin.isUpgradersDropOnDeath() && currentElement != null) {
            int currentLevel = pd.getUpgradeLevel(currentElement);

            if (currentLevel > 0) {
                for (int i = 0; i < currentLevel; i++) {
                    if (i == 0) {
                        event.getDrops().add(plugin.getItemManager().createUpgrader1());
                    } else {
                        event.getDrops().add(plugin.getItemManager().createUpgrader2());
                    }
                }

                pd.setUpgradeLevel(currentElement, 0);
                plugin.getDataStore().save(pd);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (player.isOnline()) {
                            elements.applyUpsides(player);
                        }
                    }
                }.runTaskLater(plugin, 1L);
            }
        }

        // Check if player has an advanced element
        if (currentElement != null && isAdvancedElement(currentElement)) {
            // Pick a random basic element to reroll to
            ElementType newElement = BASIC_ELEMENTS[random.nextInt(BASIC_ELEMENTS.length)];

            // Schedule reroll for after respawn
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        // Clear old advanced element effects
                        SmartEffectCleaner.clearForElementChange(plugin, player);

                        // Assign new basic element (resets upgrade level to 0)
                        pd.setCurrentElement(newElement);
                        plugin.getDataStore().save(pd);

                        // Apply new element effects
                        elements.applyUpsides(player);

                        // Notify player
                        player.sendMessage(ChatColor.RED + "You died and lost your advanced element!");
                        player.sendMessage(ChatColor.YELLOW + "Your new element is " +
                                ChatColor.AQUA + newElement.name());

                        // Play sound
                        player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 1.0f, 0.8f);

                        // Show title
                        player.sendTitle(
                                ChatColor.RED + "Element Lost",
                                ChatColor.AQUA + newElement.name(),
                                10, 40, 10
                        );
                    }
                }
            }.runTaskLater(plugin, 5L);
        }
    }

    /**
     * Check if an element is an advanced element
     * Uses the same list as AdvancedRerollerListener
     */
    private boolean isAdvancedElement(ElementType element) {
        for (ElementType advanced : ADVANCED_ELEMENTS) {
            if (advanced == element) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if an element is a basic element
     */
    private boolean isBasicElement(ElementType element) {
        for (ElementType basic : BASIC_ELEMENTS) {
            if (basic == element) {
                return true;
            }
        }
        return false;
    }
}