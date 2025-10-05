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
    private final java.util.Set<java.util.UUID> activeAbility1 = new java.util.HashSet<>();
    private final java.util.Set<java.util.UUID> activeAbility2 = new java.util.HashSet<>();
    
    public BaseElement(ElementPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean ability1(ElementContext context) {
        if (!checkUpgradeLevel(context.getPlayer(), context.getUpgradeLevel(), 1)) return false;
        
        // Check if ability is already active
        if (isAbility1Active(context.getPlayer())) {
            context.getPlayer().sendMessage(ChatColor.YELLOW + "Ability 1 is already active!");
            return false;
        }
        
        // Check cooldown
        String cooldownKey = getType().toString().toLowerCase() + "_ability1";
        if (!plugin.getCooldownManager().tryUseAbility(context.getPlayer(), cooldownKey, 3)) {
            return false;
        }

        int cost = context.getConfigManager().getAbility1Cost(getType());
        if (!hasMana(context.getPlayer(), context.getManaManager(), cost)) return false;
        
        // Execute ability first, only consume mana if successful
        if (executeAbility1(context)) {
            context.getManaManager().spend(context.getPlayer(), cost);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean ability2(ElementContext context) {
        // Require upgrade level 1 before allowing upgrade level 2
        if (context.getUpgradeLevel() < 1) {
            context.getPlayer().sendMessage(ChatColor.RED + "You need Upgrade I before you can use Upgrade II abilities.");
            return false;
        }
        
        if (!checkUpgradeLevel(context.getPlayer(), context.getUpgradeLevel(), 2)) return false;
        
        // Check if ability is already active
        if (isAbility2Active(context.getPlayer())) {
            context.getPlayer().sendMessage(ChatColor.YELLOW + "Ability 2 is already active!");
            return false;
        }
        
        // Check cooldown
        String cooldownKey = getType().toString().toLowerCase() + "_ability2";
        if (!plugin.getCooldownManager().tryUseAbility(context.getPlayer(), cooldownKey, 3)) {
            return false;
        }
        
        int cost = context.getConfigManager().getAbility2Cost(getType());
        if (!hasMana(context.getPlayer(), context.getManaManager(), cost)) return false;
        
        // Execute ability first, only consume mana if successful
        if (executeAbility2(context)) {
            context.getManaManager().spend(context.getPlayer(), cost);
            return true;
        }
        return false;
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
     * Check if player has enough mana (without spending it)
     */
    protected boolean hasMana(Player player, hs.elementPlugin.managers.ManaManager mana, int cost) {
        if (mana.get(player.getUniqueId()).getMana() < cost) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }
        return true;
    }
    
    /**
     * Check if player has enough mana and spend it (deprecated - use hasMana instead)
     */
    @Deprecated
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
    
    /**
     * Ability cooldown management methods
     */
    protected boolean isAbility1Active(Player player) {
        return activeAbility1.contains(player.getUniqueId());
    }
    
    public boolean isAbility2Active(Player player) {
        return activeAbility2.contains(player.getUniqueId());
    }
    
    protected void setAbility1Active(Player player, boolean active) {
        if (active) {
            activeAbility1.add(player.getUniqueId());
        } else {
            activeAbility1.remove(player.getUniqueId());
        }
    }
    
    protected void setAbility2Active(Player player, boolean active) {
        if (active) {
            activeAbility2.add(player.getUniqueId());
        } else {
            activeAbility2.remove(player.getUniqueId());
        }
    }
}