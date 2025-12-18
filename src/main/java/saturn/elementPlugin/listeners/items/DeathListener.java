package saturn.elementPlugin.listeners.items;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.impl.earth.EarthElement;
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

        // CRITICAL FIX: Clear active abilities on death
        clearActiveAbilities(player, currentElement);

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
     * CRITICAL FIX: Clear active abilities when player dies
     * This prevents Earth Tunnel and Water Whirlpool from staying active after respawn
     */
    private void clearActiveAbilities(Player player, ElementType element) {
        if (element == null) return;

        // Clear Earth Tunnel metadata
        if (element == ElementType.EARTH) {
            player.removeMetadata(EarthElement.META_TUNNELING, plugin);
        }

        // Clear ability active states for ALL elements
        var abilityManager = plugin.getAbilityManager();
        if (abilityManager != null) {
            // Get abilities for this element
            for (int abilityNum = 1; abilityNum <= 2; abilityNum++) {
                String abilityId = getAbilityId(element, abilityNum);
                var ability = abilityManager.getAbility(abilityId);
                if (ability != null) {
                    ability.setActive(player, false);
                }
            }
        }
    }

    /**
     * Get ability ID for clearing active states
     */
    private String getAbilityId(ElementType elementType, int abilityNumber) {
        switch (elementType) {
            case AIR:
                return abilityNumber == 1 ? "air_blast" : "air_dash";
            case WATER:
                return abilityNumber == 1 ? "water_whirlpool" : "water_prison";
            case FIRE:
                return abilityNumber == 1 ? "fire_hellish_flames" : "fire_phoenix_form";
            case EARTH:
                return abilityNumber == 1 ? "earth_tunnel" : "earth_earthquake";
            case LIFE:
                return abilityNumber == 1 ? "life_regen" : "transfusion";
            case DEATH:
                return abilityNumber == 1 ? "death_slash" : "death_clock";
            case METAL:
                return abilityNumber == 1 ? "metal_dash" : "metal_chain";
            case FROST:
                return abilityNumber == 1 ? "ice_shard_volley" : "frost_nova";
            default:
                return elementType.name().toLowerCase() + "_ability_" + abilityNumber;
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