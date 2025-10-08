package hs.elementPlugin.elements.abilities;

import hs.elementPlugin.elements.ElementContext;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Base implementation of the Ability interface that handles common functionality
 */
public abstract class BaseAbility implements Ability {
    private final String abilityId;
    private final int manaCost;
    private final int cooldownSeconds;
    private final int requiredUpgradeLevel;
    private final Set<UUID> activePlayers = new HashSet<>();
    
    /**
     * Create a new ability
     * 
     * @param abilityId The unique identifier for this ability
     * @param manaCost The mana cost for this ability
     * @param cooldownSeconds The cooldown in seconds
     * @param requiredUpgradeLevel The minimum upgrade level required
     */
    public BaseAbility(String abilityId, int manaCost, int cooldownSeconds, int requiredUpgradeLevel) {
        this.abilityId = abilityId;
        this.manaCost = manaCost;
        this.cooldownSeconds = cooldownSeconds;
        this.requiredUpgradeLevel = requiredUpgradeLevel;
    }
    
    @Override
    public int getManaCost() {
        return manaCost;
    }
    
    @Override
    public int getCooldownSeconds() {
        return cooldownSeconds;
    }
    
    @Override
    public int getRequiredUpgradeLevel() {
        return requiredUpgradeLevel;
    }
    
    @Override
    public String getAbilityId() {
        return abilityId;
    }
    
    @Override
    public boolean isActiveFor(Player player) {
        return activePlayers.contains(player.getUniqueId());
    }
    
    @Override
    public void setActive(Player player, boolean active) {
        if (active) {
            activePlayers.add(player.getUniqueId());
        } else {
            activePlayers.remove(player.getUniqueId());
        }
    }

    public abstract String getName();

    public abstract String getDescription();

    /**
     * Helper method to check if a target is valid for an ability.
     * Override in subclasses for more complex logic.
     */
    protected boolean isValidTarget(ElementContext context, org.bukkit.entity.LivingEntity target) {
        return !target.equals(context.getPlayer());
    }
}