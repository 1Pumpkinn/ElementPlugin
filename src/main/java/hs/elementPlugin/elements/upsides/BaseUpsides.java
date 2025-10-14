package hs.elementPlugin.elements.upsides;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;

/**
 * Base class for element upsides
 * Provides common functionality for all element upside implementations
 */
public abstract class BaseUpsides {
    protected final ElementManager elementManager;

    public BaseUpsides(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    /**
     * Apply all upsides for this element to a player
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for this element
     */
    public abstract void applyUpsides(Player player, int upgradeLevel);

    /**
     * Get the element type this upside class handles
     * @return The ElementType
     */
    public abstract ElementType getElementType();

    /**
     * Check if a player has this element
     * @param player The player to check
     * @return true if the player has this element
     */
    public boolean hasElement(Player player) {
        return elementManager.getPlayerElement(player) == getElementType();
    }

    /**
     * Get the upgrade level for this element for a player
     * @param player The player to check
     * @return The upgrade level
     */
    protected int getUpgradeLevel(Player player) {
        var pd = elementManager.data(player.getUniqueId());
        if (pd == null) return 0;
        return pd.getUpgradeLevel(getElementType());
    }
}
