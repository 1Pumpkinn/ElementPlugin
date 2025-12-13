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
 * Properly handles both basic and advanced element effects
 * FIXED: Now includes extensive logging to catch effect glitches
 */
public class SmartEffectCleaner {

    // Map of which infinite effects belong to which elements
    private static final Map<ElementType, Set<PotionEffectType>> ELEMENT_EFFECTS = new EnumMap<>(ElementType.class);

    // Threshold for considering an effect "infinite" (effects longer than this are element passives)
    private static final int INFINITE_EFFECT_THRESHOLD = 1_000_000;

    static {
        // BASIC ELEMENTS (Reroller)

        // Water: Conduit Power
        ELEMENT_EFFECTS.put(ElementType.WATER, Set.of(PotionEffectType.CONDUIT_POWER));

        // Fire: Fire Resistance
        ELEMENT_EFFECTS.put(ElementType.FIRE, Set.of(PotionEffectType.FIRE_RESISTANCE));

        // Earth: Hero of the Village
        ELEMENT_EFFECTS.put(ElementType.EARTH, Set.of(PotionEffectType.HERO_OF_THE_VILLAGE));

        // Air: No infinite potion effects
        ELEMENT_EFFECTS.put(ElementType.AIR, Set.of());

        // ADVANCED ELEMENTS (Advanced Reroller)

        // Life: Regeneration (infinite)
        ELEMENT_EFFECTS.put(ElementType.LIFE, Set.of(PotionEffectType.REGENERATION));

        // Metal: Resistance (infinite)
        ELEMENT_EFFECTS.put(ElementType.METAL, Set.of(PotionEffectType.RESISTANCE));

        // Death: No infinite potion effects
        ELEMENT_EFFECTS.put(ElementType.DEATH, Set.of());

        // Frost: No infinite potion effects (Speed is applied dynamically by FrostPassiveListener)
        ELEMENT_EFFECTS.put(ElementType.FROST, Set.of());
    }

    /**
     * Clean ONLY the infinite effects that don't belong to the current element
     * This preserves correct element effects while removing old ones
     * ONLY removes effects with duration > INFINITE_EFFECT_THRESHOLD (1 million ticks)
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
            // CRITICAL: Only check INFINITE effects (duration > threshold)
            if (effect.getDuration() > INFINITE_EFFECT_THRESHOLD) {
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
     * Clear ALL element effects for element change (reroller/advanced reroller)
     * This is used when switching elements - clears old infinite effects, applies new
     * ONLY removes INFINITE effects (duration > threshold), preserves timed effects
     * FIXED: Added comprehensive logging
     */
    public static void clearForElementChange(ElementPlugin plugin, Player player) {
        int clearedCount = 0;

        plugin.getLogger().info("=== Starting clearForElementChange for " + player.getName() + " ===");

        // Get all active effects BEFORE clearing
        Collection<PotionEffect> activeEffects = new ArrayList<>(player.getActivePotionEffects());

        plugin.getLogger().info("Active effects BEFORE clearing:");
        for (PotionEffect effect : activeEffects) {
            plugin.getLogger().info("  - " + effect.getType() + " (duration: " + effect.getDuration() + ", amplifier: " + effect.getAmplifier() + ")");
        }

        // Clear ONLY element-specific INFINITE effects
        for (PotionEffect effect : activeEffects) {
            // CRITICAL: Only remove INFINITE effects
            if (effect.getDuration() > INFINITE_EFFECT_THRESHOLD) {
                PotionEffectType type = effect.getType();

                // Check if this is an element effect
                boolean isElementEffect = ELEMENT_EFFECTS.values().stream()
                        .anyMatch(set -> set.contains(type));

                if (isElementEffect) {
                    player.removePotionEffect(type);
                    plugin.getLogger().info("  ✓ Cleared infinite element effect: " + type + " (duration: " + effect.getDuration() + ")");
                    clearedCount++;
                }
            }
        }

        // Clear element-specific metadata
        clearElementMetadata(plugin, player);

        // Reset ALL attributes to default (will be set correctly after)
        resetAttributesToDefault(player);

        // Clear temporary effects (these are always safe to clear)
        player.setFireTicks(0);
        player.setFreezeTicks(0);

        plugin.getLogger().info("=== Cleared " + clearedCount + " infinite element effects from " + player.getName() + " ===");

        // Log remaining effects AFTER clearing
        Collection<PotionEffect> remainingEffects = player.getActivePotionEffects();
        plugin.getLogger().info("Remaining effects AFTER clearing:");
        for (PotionEffect effect : remainingEffects) {
            plugin.getLogger().info("  - " + effect.getType() + " (duration: " + effect.getDuration() + ")");
        }
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
     * Reset all element-related attributes to default values
     */
    private static void resetAttributesToDefault(Player player) {
        // Max Health (reset to 20 - Life will set to 30 if needed)
        var healthAttr = player.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getBaseValue() != 20.0) {
            double currentHealth = player.getHealth();
            healthAttr.setBaseValue(20.0);
            if (!player.isDead() && currentHealth > 0) {
                player.setHealth(Math.min(currentHealth, 20.0));
            }
        }

        // Underwater Mining Speed (reset to 0.2 - Water will set to 1.2 if needed)
        var miningAttr = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED);
        if (miningAttr != null && miningAttr.getBaseValue() != 0.2) {
            miningAttr.setBaseValue(0.2);
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

    /**
     * Check if an effect is considered "infinite" (element passive)
     */
    public static boolean isInfiniteEffect(PotionEffect effect) {
        return effect.getDuration() > INFINITE_EFFECT_THRESHOLD;
    }
}