package hs.elementPlugin.elements.abilities;

import hs.elementPlugin.elements.ElementContext;
import org.bukkit.entity.Player;

/**
 * Interface for all element abilities
 */
public interface Ability {
    /**
     * Execute the ability
     * @param context The context for the ability execution
     * @return true if the ability was executed successfully, false otherwise
     */
    boolean execute(ElementContext context);
    
    /**
     * Get the mana cost for this ability
     * @return The mana cost
     */
    int getManaCost();
    
    /**
     * Get the cooldown for this ability in seconds
     * @return The cooldown in seconds
     */
    int getCooldownSeconds();
    
    /**
     * Get the minimum upgrade level required for this ability
     * @return The minimum upgrade level
     */
    int getRequiredUpgradeLevel();
    
    /**
     * Get the ability ID used for cooldown tracking
     * @return The ability ID
     */
    String getAbilityId();
    
    /**
     * Check if the ability is currently active for the player
     * @param player The player to check
     * @return true if the ability is active, false otherwise
     */
    boolean isActiveFor(Player player);
    
    /**
     * Set the active state for this ability
     * @param player The player
     * @param active Whether the ability is active
     */
    void setActive(Player player, boolean active);

    /**
     * Get the display name of this ability
     * @return The name
     */
    String getName();

    /**
     * Get the description of this ability
     * @return The description
     */
    String getDescription();
}