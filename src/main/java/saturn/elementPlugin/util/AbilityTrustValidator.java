package saturn.elementPlugin.util;

import saturn.elementPlugin.ElementPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Centralized utility for checking trust in abilities
 * This ensures ALL abilities respect the trust system AND disabled regions
 * FIXED: Now shows correct message for spawn protection vs trust
 */
public class AbilityTrustValidator {

    private static final Map<UUID, Long> messageCooldowns = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000; // 3 seconds

    /**
     * Check if an ability can affect a target based on trust AND disabled regions
     * PRIORITY: Spawn protection is checked BEFORE trust to show correct message
     *
     * @param plugin The plugin instance
     * @param caster The player using the ability
     * @param target The target entity
     * @param sendMessage Whether to send feedback message to caster
     * @return true if ability can affect target, false if trust or zone prevents it
     */
    public static boolean canAffectTarget(ElementPlugin plugin, Player caster, LivingEntity target, boolean sendMessage) {
        // Can always affect non-players
        if (!(target instanceof Player targetPlayer)) {
            return true;
        }

        // Can't affect yourself (most abilities handle this separately)
        if (targetPlayer.equals(caster)) {
            return false;
        }

        // === PRIORITY 1: Check spawn protection FIRST (before trust check) ===
        boolean isInSpawn = plugin.getDisabledRegionsManager().isInDisabledRegion(targetPlayer.getLocation());

        // DEBUG: Log the check
        plugin.getLogger().info("[AbilityTrust] Checking " + caster.getName() + " -> " + targetPlayer.getName());
        plugin.getLogger().info("[AbilityTrust] Target in spawn: " + isInSpawn);

        if (isInSpawn) {
            // Target is in spawn - IMMEDIATELY return false with spawn message
            if (sendMessage && !isOnMessageCooldown(caster)) {
                String regionName = plugin.getDisabledRegionsManager().getRegionNameAt(targetPlayer.getLocation());
                caster.sendMessage(ChatColor.RED + "You cannot affect " + targetPlayer.getName() +
                        " - they are in a protected zone!" +
                        (regionName != null ? " (" + regionName + ")" : ""));
                setMessageCooldown(caster);
            }
            plugin.getLogger().info("[AbilityTrust] BLOCKED: Spawn protection");
            return false; // STOP HERE - don't check trust
        }

        // === PRIORITY 2: Check trust system (only after spawn check passed) ===
        boolean trusts = plugin.getTrustManager().trusts(caster, targetPlayer);
        plugin.getLogger().info("[AbilityTrust] Caster trusts target: " + trusts);

        if (trusts) {
            if (sendMessage && !isOnMessageCooldown(caster)) {
                caster.sendMessage(ChatColor.YELLOW + "Your ability cannot affect " +
                        targetPlayer.getName() + " - you trust them!");
                setMessageCooldown(caster);
            }
            plugin.getLogger().info("[AbilityTrust] BLOCKED: Trust system");
            return false;
        }

        // All checks passed - can affect target
        plugin.getLogger().info("[AbilityTrust] ALLOWED: Can affect target");
        return true;
    }

    /**
     * Convenience method with message enabled by default
     */
    public static boolean canAffectTarget(ElementPlugin plugin, Player caster, LivingEntity target) {
        return canAffectTarget(plugin, caster, target, true);
    }

    /**
     * Filter a collection of targets to only include those that can be affected
     * UPDATED: Now also filters out players in disabled regions
     *
     * @param plugin The plugin instance
     * @param caster The player using the ability
     * @param targets Collection of potential targets
     * @return List of valid targets (excluding trusted players and players in disabled regions)
     */
    public static List<LivingEntity> filterValidTargets(ElementPlugin plugin, Player caster, Collection<LivingEntity> targets) {
        List<LivingEntity> validTargets = new ArrayList<>();

        for (LivingEntity target : targets) {
            if (canAffectTarget(plugin, caster, target, false)) {
                validTargets.add(target);
            }
        }

        return validTargets;
    }

    /**
     * Check if two players mutually trust each other
     */
    public static boolean hasMutualTrust(ElementPlugin plugin, Player player1, Player player2) {
        return plugin.getTrustManager().mutualTrust(player1, player2);
    }

    /**
     * Check if a target is in a disabled region (spawn protection)
     * This is a convenience method for abilities that need to check zone protection
     */
    public static boolean isInProtectedZone(ElementPlugin plugin, LivingEntity target) {
        if (!(target instanceof Player)) {
            return false;
        }
        return plugin.getDisabledRegionsManager().isInDisabledRegion(target.getLocation());
    }

    /**
     * Check if EITHER the caster OR the target is in a disabled region
     * Useful for abilities that should be completely blocked if either party is in spawn
     */
    public static boolean eitherInProtectedZone(ElementPlugin plugin, Player caster, LivingEntity target) {
        // Check if caster is in a disabled region (handled elsewhere, but double-check)
        if (plugin.getDisabledRegionsManager().isInDisabledRegion(caster.getLocation())) {
            return true;
        }

        // Check if target is in a disabled region
        return isInProtectedZone(plugin, target);
    }

    // Message cooldown management to prevent spam

    private static boolean isOnMessageCooldown(Player player) {
        Long lastMessage = messageCooldowns.get(player.getUniqueId());
        if (lastMessage == null) return false;

        return System.currentTimeMillis() - lastMessage < MESSAGE_COOLDOWN_MS;
    }

    private static void setMessageCooldown(Player player) {
        messageCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * Clean up old cooldown entries
     * Should be called periodically by the plugin
     */
    public static void cleanupCooldowns() {
        long now = System.currentTimeMillis();
        messageCooldowns.entrySet().removeIf(entry ->
                now - entry.getValue() > MESSAGE_COOLDOWN_MS
        );
    }
}