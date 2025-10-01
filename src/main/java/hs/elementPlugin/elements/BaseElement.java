package hs.elementPlugin.elements;

import hs.elementPlugin.ElementPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * Abstract base class for all elements that provides common functionality
 * and reduces code duplication in element implementations.
 */
public abstract class BaseElement implements Element {
    protected final ElementPlugin plugin;
    
    public BaseElement(ElementPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean ability1(ElementContext context) {
        if (!checkUpgradeLevel(context.getPlayer(), context.getUpgradeLevel(), 1)) return false;
        
        int cost = context.getConfigManager().getAbility1Cost(getType());
        if (!checkMana(context.getPlayer(), context.getManaManager(), cost)) return false;
        
        return executeAbility1(context);
    }
    
    @Override
    public boolean ability2(ElementContext context) {
        if (!checkUpgradeLevel(context.getPlayer(), context.getUpgradeLevel(), 2)) return false;
        
        int cost = context.getConfigManager().getAbility2Cost(getType());
        if (!checkMana(context.getPlayer(), context.getManaManager(), cost)) return false;
        
        return executeAbility2(context);
    }
    
    /**
     * Check if player has required upgrade level for ability
     */
    protected boolean checkUpgradeLevel(Player player, int upgradeLevel, int requiredLevel) {
        if (upgradeLevel < requiredLevel) {
            player.sendMessage(ChatColor.RED + "You need Upgrade " + 
                (requiredLevel == 1 ? "I" : "II") + " to use this ability.");
            return false;
        }
        return true;
    }
    
    /**
     * Check if player has enough mana and spend it
     */
    protected boolean checkMana(Player player, hs.elementPlugin.managers.ManaManager mana, int cost) {
        if (!mana.spend(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }
        return true;
    }
    
    /**
     * Template methods to be implemented by concrete elements
     */
    protected abstract boolean executeAbility1(ElementContext context);
    
    protected abstract boolean executeAbility2(ElementContext context);
    
    /**
     * Helper method to check if target is valid (not player or not trusted)
     */
    protected boolean isValidTarget(Player player, org.bukkit.entity.LivingEntity target, hs.elementPlugin.managers.TrustManager trust) {
        if (target.equals(player)) return false;
        if (target instanceof Player other && trust.isTrusted(player.getUniqueId(), other.getUniqueId())) {
            return false;
        }
        return true;
    }
    
    /**
     * Helper method to check if target is valid using ElementContext
     */
    protected boolean isValidTarget(ElementContext context, org.bukkit.entity.LivingEntity target) {
        return isValidTarget(context.getPlayer(), target, context.getTrustManager());
    }
}