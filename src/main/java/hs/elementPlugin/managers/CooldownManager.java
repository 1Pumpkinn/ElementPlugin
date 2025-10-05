package hs.elementPlugin.managers;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages cooldowns for element abilities
 */
public class CooldownManager {
    // Map of player UUID to ability cooldowns (ability name -> expiration time)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    
    /**
     * Check if a player's ability is on cooldown
     * 
     * @param player The player to check
     * @param abilityName The ability name (e.g., "air_ability1")
     * @return true if the ability is on cooldown, false otherwise
     */
    public boolean isOnCooldown(Player player, String abilityName) {
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId)) return false;
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (!playerCooldowns.containsKey(abilityName)) return false;
        
        long expiration = playerCooldowns.get(abilityName);
        return System.currentTimeMillis() < expiration;
    }
    
    /**
     * Get the remaining cooldown time in seconds
     * 
     * @param player The player to check
     * @param abilityName The ability name
     * @return Remaining cooldown in seconds, or 0 if not on cooldown
     */
    public int getRemainingCooldown(Player player, String abilityName) {
        UUID playerId = player.getUniqueId();
        if (!cooldowns.containsKey(playerId)) return 0;
        
        Map<String, Long> playerCooldowns = cooldowns.get(playerId);
        if (!playerCooldowns.containsKey(abilityName)) return 0;
        
        long expiration = playerCooldowns.get(abilityName);
        long remaining = expiration - System.currentTimeMillis();
        
        return remaining > 0 ? (int) (remaining / 1000) : 0;
    }
    
    /**
     * Set a cooldown for a player's ability
     * 
     * @param player The player
     * @param abilityName The ability name
     * @param cooldownSeconds The cooldown duration in seconds
     */
    public void setCooldown(Player player, String abilityName, int cooldownSeconds) {
        UUID playerId = player.getUniqueId();
        
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>());
        
        long expiration = System.currentTimeMillis() + (cooldownSeconds * 1000L);
        cooldowns.get(playerId).put(abilityName, expiration);
    }
    
    /**
     * Try to use an ability, checking cooldown first
     * 
     * @param player The player
     * @param abilityName The ability name
     * @param cooldownSeconds The cooldown to apply if used
     * @return true if the ability can be used, false if on cooldown
     */
    public boolean tryUseAbility(Player player, String abilityName, int cooldownSeconds) {
        if (isOnCooldown(player, abilityName)) {
            int remaining = getRemainingCooldown(player, abilityName);
            player.sendMessage(ChatColor.RED + "This ability is on cooldown for " + remaining + " more seconds!");
            return false;
        }
        
        // Set the cooldown
        setCooldown(player, abilityName, cooldownSeconds);
        return true;
    }
    
    /**
     * Clear all cooldowns for a player (e.g., on disconnect)
     * 
     * @param playerId The player's UUID
     */
    public void clearCooldowns(UUID playerId) {
        cooldowns.remove(playerId);
    }
}