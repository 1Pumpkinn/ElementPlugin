package hs.elementPlugin.util;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Smart utility class for cleaning up element effects
 * Only removes infinite effects that DON'T belong to the current element
 */
public class SmartEffectCleaner {

    // Map of which infinite effects belong to which elements
    private static final Map<ElementType, Set<PotionEffectType>> ELEMENT_EFFECTS = new EnumMap<>(ElementType.class);

    static {
        // Water: Conduit Power
        ELEMENT_EFFECTS.put(ElementType.WATER, Set.of(PotionEffectType.CONDUIT_POWER));

        // Fire: Fire Resistance
        ELEMENT_EFFECTS.put(ElementType.FIRE, Set.of(PotionEffectType.FIRE_RESISTANCE));

        // Earth: Hero of the Village
        ELEMENT_EFFECTS.put(ElementType.EARTH, Set.of(PotionEffectType.HERO_OF_THE_VILLAGE));

        // Life: Regeneration
        ELEMENT_EFFECTS.put(ElementType.LIFE, Set.of(PotionEffectType.REGENERATION));

        // Metal: Resistance
        ELEMENT_EFFECTS.put(ElementType.METAL, Set.of(PotionEffectType.RESISTANCE));

        // Air, Death, Frost have no infinite potion effects
        ELEMENT_EFFECTS.put(ElementType.AIR, Set.of());
        ELEMENT_EFFECTS.put(ElementType.DEATH, Set.of());
        ELEMENT_EFFECTS.put(ElementType.FROST, Set.of());
    }

    /**
     * Clean ONLY the infinite effects that don't belong to the current element
     * This preserves correct element effects while removing old ones
     */
    public static void cleanInvalidInfiniteEffects(ElementPlugin plugin, Player player) {
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement == null) {
            plugin.getLogger().warning("Attempted to clean effects for " + player.getName() + " with no element");
            return;
        }

        // Get the effects that SHOULD be on this player
        Set<PotionEffectType> validEffects = ELEMENT_EFFECTS.getOrDefault(currentElement, Set.of());

        // Check all active potion effects
        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();

        for (PotionEffect effect : activeEffects) {
            // Only check infinite effects (duration > 1 million ticks)
            if (effect.getDuration() > 1_000_000) {
                PotionEffectType type = effect.getType();

                // If this infinite effect is NOT in the valid set for current element, remove it
                if (!validEffects.contains(type)) {
                    // Check if it belongs to ANY element
                    boolean belongsToAnyElement = ELEMENT_EFFECTS.values().stream()
                            .anyMatch(set -> set.contains(type));

                    if (belongsToAnyElement) {
                        player.removePotionEffect(type);
                        plugin.getLogger().info("Removed invalid infinite effect " + type + " from " + player.getName() + " (Current: " + currentElement + ")");
                    }
                }
            }
        }
    }

    /**
     * Clear ALL element effects for element change (advanced reroller)
     * This is used when switching elements - clears old, applies new
     */
    public static void clearForElementChange(ElementPlugin plugin, Player player) {
        // Clear ALL element-specific infinite effects
        for (Set<PotionEffectType> effectSet : ELEMENT_EFFECTS.values()) {
            for (PotionEffectType type : effectSet) {
                player.removePotionEffect(type);
            }
        }

        // Clear element-specific metadata
        clearElementMetadata(plugin, player);

        // Reset ALL attributes to default (will be set correctly after)
        var healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getBaseValue() != 20.0) {
            double currentHealth = player.getHealth();
            healthAttr.setBaseValue(20.0);
            if (!player.isDead() && currentHealth > 0) {
                player.setHealth(Math.min(currentHealth, 20.0));
            }
        }

        var miningAttr = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED);
        if (miningAttr != null) {
            miningAttr.setBaseValue(0.2);
        }

        // Clear temporary effects
        player.setFireTicks(0);
        player.setFreezeTicks(0);

        plugin.getLogger().fine("Cleared all element effects for " + player.getName() + " (element change)");
    }

    /**
     * Clear element-specific metadata from player
     */
    private static void clearElementMetadata(ElementPlugin plugin, Player player) {
        // Clear ALL element-related metadata using ElementManager
        for (ElementType type : ElementType.values()) {
            Element element = plugin.getElementManager().get(type);
            if (element != null) {
                try {
                    element.clearEffects(player);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error clearing " + type + " effects: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Reset player attributes based on element
     */
    private static void resetAttributes(Player player, ElementType currentElement) {
        // Max Health (only Life should have 30)
        var healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null) {
            double targetHealth = (currentElement == ElementType.LIFE) ? 30.0 : 20.0;

            if (healthAttr.getBaseValue() != targetHealth) {
                double currentHealth = player.getHealth();
                healthAttr.setBaseValue(targetHealth);

                // Cap health if needed
                if (!player.isDead() && currentHealth > 0) {
                    player.setHealth(Math.min(currentHealth, targetHealth));
                }
            }
        }

        // Underwater Mining Speed (only Water with Upgrade 2 should have 1.2)
        var miningAttr = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED);
        if (miningAttr != null) {
            // Will be set correctly by applyUpsides if needed
            if (currentElement != ElementType.WATER) {
                miningAttr.setBaseValue(0.2);
            }
        }
    }

    /**
     * Check if a potion effect type belongs to any element
     */
    public static boolean isElementEffect(PotionEffectType type) {
        return ELEMENT_EFFECTS.values().stream()
                .anyMatch(set -> set.contains(type));
    }

    /**
     * Get all effect types that belong to a specific element
     */
    public static Set<PotionEffectType> getElementEffects(ElementType element) {
        return new HashSet<>(ELEMENT_EFFECTS.getOrDefault(element, Set.of()));
    }
}