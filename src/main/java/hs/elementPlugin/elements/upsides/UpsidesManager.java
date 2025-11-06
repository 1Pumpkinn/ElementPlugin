package hs.elementPlugin.elements.upsides;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.impl.*;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manager class for all element upsides
 * Centralizes upside management and provides easy access to element-specific upside classes
 */
public class UpsidesManager {
    private final Map<ElementType, BaseUpsides> upsidesMap;
    private final ElementManager elementManager;

    public UpsidesManager(ElementManager elementManager) {
        this.elementManager = elementManager;
        this.upsidesMap = new HashMap<>();
        initializeUpsides();
    }

    /**
     * Initialize all element upside classes
     */
    private void initializeUpsides() {
        upsidesMap.put(ElementType.AIR, new AirUpsides(elementManager));
        upsidesMap.put(ElementType.WATER, new WaterUpsides(elementManager));
        upsidesMap.put(ElementType.FIRE, new FireUpsides(elementManager));
        upsidesMap.put(ElementType.EARTH, new EarthUpsides(elementManager));
        upsidesMap.put(ElementType.LIFE, new LifeUpsides(elementManager));
        upsidesMap.put(ElementType.DEATH, new DeathUpsides(elementManager));
        upsidesMap.put(ElementType.METAL, new MetalUpsides(elementManager));
        upsidesMap.put(ElementType.FROST, new FrostUpsides(elementManager));
    }

    /**
     * Get the upside class for a specific element
     * @param elementType The element type
     * @return The upside class, or null if not found
     */
    public BaseUpsides getUpsides(ElementType elementType) {
        return upsidesMap.get(elementType);
    }

    /**
     * Apply upsides for a specific element to a player
     * @param elementType The element type
     * @param player The player to apply upsides to
     * @param upgradeLevel The upgrade level
     */
    public void applyUpsides(ElementType elementType, Player player, int upgradeLevel) {
        BaseUpsides upsides = getUpsides(elementType);
        if (upsides != null) {
            upsides.applyUpsides(player, upgradeLevel);
        }
    }

    /**
     * Apply upsides for a player's current element
     * @param player The player to apply upsides to
     */
    public void applyUpsidesForPlayer(Player player) {
        var pd = elementManager.data(player.getUniqueId());
        if (pd == null || pd.getCurrentElement() == null) return;

        ElementType elementType = pd.getCurrentElement();
        int upgradeLevel = pd.getUpgradeLevel(elementType);
        applyUpsides(elementType, player, upgradeLevel);
    }

    /**
     * Get Air upsides (convenience method)
     * @return AirUpsides instance
     */
    public AirUpsides getAirUpsides() {
        return (AirUpsides) getUpsides(ElementType.AIR);
    }

    /**
     * Get Water upsides (convenience method)
     * @return WaterUpsides instance
     */
    public WaterUpsides getWaterUpsides() {
        return (WaterUpsides) getUpsides(ElementType.WATER);
    }

    /**
     * Get Fire upsides (convenience method)
     * @return FireUpsides instance
     */
    public FireUpsides getFireUpsides() {
        return (FireUpsides) getUpsides(ElementType.FIRE);
    }

    /**
     * Get Earth upsides (convenience method)
     * @return EarthUpsides instance
     */
    public EarthUpsides getEarthUpsides() {
        return (EarthUpsides) getUpsides(ElementType.EARTH);
    }

    /**
     * Get Life upsides (convenience method)
     * @return LifeUpsides instance
     */
    public LifeUpsides getLifeUpsides() {
        return (LifeUpsides) getUpsides(ElementType.LIFE);
    }

    /**
     * Get Death upsides (convenience method)
     * @return DeathUpsides instance
     */
    public DeathUpsides getDeathUpsides() {
        return (DeathUpsides) getUpsides(ElementType.DEATH);
    }

    /**
     * Get Metal upsides (convenience method)
     * @return MetalUpsides instance
     */
    public MetalUpsides getMetalUpsides() {
        return (MetalUpsides) getUpsides(ElementType.METAL);
    }

    public FrostUpsides getFrostUpsides() {
        return (FrostUpsides) getUpsides(ElementType.FROST);
    }

}