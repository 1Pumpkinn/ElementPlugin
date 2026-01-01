package saturn.elementPlugin.util;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.Element;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.Ability;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Smart utility class for cleaning up element effects
 * UPDATED: Added Speed for Air, Dolphin's Grace for Water, removed underwater mining
 */
public class SmartEffectCleaner {

    // Map of which infinite effects belong to which elements
    private static final Map<ElementType, Set<PotionEffectType>> ELEMENT_EFFECTS = new EnumMap<>(ElementType.class);

    // Threshold for considering an effect "infinite" (effects longer than this are element passives)
    private static final int INFINITE_EFFECT_THRESHOLD = 1_000_000;

    static {
        // BASIC ELEMENTS (Reroller)

        // Water: Conduit Power + Dolphin's Grace (Upgrade II)
        ELEMENT_EFFECTS.put(ElementType.WATER, Set.of(PotionEffectType.CONDUIT_POWER, PotionEffectType.DOLPHINS_GRACE));

        // Fire: Fire Resistance
        ELEMENT_EFFECTS.put(ElementType.FIRE, Set.of(PotionEffectType.FIRE_RESISTANCE));

        // Earth: No infinite potion effects
        ELEMENT_EFFECTS.put(ElementType.EARTH, Set.of());

        // Air: Speed I
        ELEMENT_EFFECTS.put(ElementType.AIR, Set.of(PotionEffectType.SPEED));

        // ADVANCED ELEMENTS (Advanced Reroller)

        // Life: No infinite potion effects (hunger drain is handled by listener)
        ELEMENT_EFFECTS.put(ElementType.LIFE, Set.of());

        // Metal: Haste (infinite)
        ELEMENT_EFFECTS.put(ElementType.METAL, Set.of(PotionEffectType.HASTE));

        // Death: No infinite potion effects
        ELEMENT_EFFECTS.put(ElementType.DEATH, Set.of());

        // Frost: Speed (dynamic, applied by FrostPassiveListener)
        ELEMENT_EFFECTS.put(ElementType.FROST, Set.of(PotionEffectType.SPEED));
    }

    /**
     * Clean ONLY the infinite effects that don't belong to the current element
     */
    public static void cleanInvalidInfiniteEffects(ElementPlugin plugin, Player player) {
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement == null) {
            plugin.getLogger().warning("Attempted to clean effects for " + player.getName() + " with no element");
            return;
        }

        Set<PotionEffectType> validEffects = ELEMENT_EFFECTS.getOrDefault(currentElement, Set.of());
        Collection<PotionEffect> activeEffects = player.getActivePotionEffects();

        for (PotionEffect effect : activeEffects) {
            if (effect.getDuration() > INFINITE_EFFECT_THRESHOLD) {
                PotionEffectType type = effect.getType();

                if (!validEffects.contains(type)) {
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
     * Clear ALL element effects for element change
     */
    public static void clearForElementChange(ElementPlugin plugin, Player player) {
        int clearedCount = 0;

        plugin.getLogger().info("=== Starting clearForElementChange for " + player.getName() + " ===");

        Collection<PotionEffect> activeEffects = new ArrayList<>(player.getActivePotionEffects());

        plugin.getLogger().info("Active effects BEFORE clearing:");
        for (PotionEffect effect : activeEffects) {
            plugin.getLogger().info("  - " + effect.getType() + " (duration: " + effect.getDuration() + ", amplifier: " + effect.getAmplifier() + ")");
        }

        for (PotionEffect effect : activeEffects) {
            if (effect.getDuration() > INFINITE_EFFECT_THRESHOLD) {
                PotionEffectType type = effect.getType();

                boolean isElementEffect = ELEMENT_EFFECTS.values().stream()
                        .anyMatch(set -> set.contains(type));

                if (isElementEffect) {
                    player.removePotionEffect(type);
                    plugin.getLogger().info("  ✓ Cleared infinite element effect: " + type + " (duration: " + effect.getDuration() + ")");
                    clearedCount++;
                }
            }
        }

        clearElementMetadata(plugin, player);
        clearAllAbilityStates(plugin, player);
        resetAttributesToDefault(player);

        player.setFireTicks(0);
        player.setFreezeTicks(0);

        plugin.getLogger().info("=== Cleared " + clearedCount + " infinite element effects from " + player.getName() + " ===");

        Collection<PotionEffect> remainingEffects = player.getActivePotionEffects();
        plugin.getLogger().info("Remaining effects AFTER clearing:");
        for (PotionEffect effect : remainingEffects) {
            plugin.getLogger().info("  - " + effect.getType() + " (duration: " + effect.getDuration() + ")");
        }
    }

    private static void clearElementMetadata(ElementPlugin plugin, Player player) {
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

    private static void clearAllAbilityStates(ElementPlugin plugin, Player player) {
        var abilityManager = plugin.getAbilityManager();

        for (ElementType elementType : ElementType.values()) {
            for (int abilityNumber = 1; abilityNumber <= 2; abilityNumber++) {
                try {
                    String abilityId = getAbilityId(elementType, abilityNumber);
                    Ability ability = abilityManager.getAbility(abilityId);

                    if (ability != null) {
                        ability.setActive(player, false);
                        plugin.getLogger().fine("Cleared ability state: " + abilityId + " for " + player.getName());
                    }
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error clearing ability state for " + elementType + " ability " + abilityNumber + ": " + ex.getMessage());
                }
            }
        }

        plugin.getLogger().info("Cleared ALL ability active states for " + player.getName());
    }

    private static String getAbilityId(ElementType elementType, int abilityNumber) {
        String elementName = elementType.name().toLowerCase();

        switch (elementType) {
            case AIR:
                return abilityNumber == 1 ? "air_blast" : "air_dash";
            case WATER:
                return abilityNumber == 1 ? "water_whirlpool" : "water_prison";
            case FIRE:
                return abilityNumber == 1 ? "fire_hellish_flames" : "fire_phoenix_form";
            case EARTH:
                return abilityNumber == 1 ? "earth_mining_frenzy" : "earth_earthquake";
            case LIFE:
                return abilityNumber == 1 ? "life_regen" : "transfusion";
            case DEATH:
                return abilityNumber == 1 ? "death_slash" : "death_clock";
            case METAL:
                return abilityNumber == 1 ? "metal_dash" : "metal_chain";
            case FROST:
                return abilityNumber == 1 ? "ice_shard_volley" : "frost_nova";
            default:
                return elementName + "_ability_" + abilityNumber;
        }
    }

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
    }

    public static boolean isElementEffect(PotionEffectType type) {
        return ELEMENT_EFFECTS.values().stream()
                .anyMatch(set -> set.contains(type));
    }

    public static Set<PotionEffectType> getElementEffects(ElementType element) {
        return new HashSet<>(ELEMENT_EFFECTS.getOrDefault(element, Set.of()));
    }

    public static boolean isInfiniteEffect(PotionEffect effect) {
        return effect.getDuration() > INFINITE_EFFECT_THRESHOLD;
    }
}