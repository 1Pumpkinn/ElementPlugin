package saturn.elementPlugin.util;

import saturn.elementPlugin.ElementPlugin;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Centralized utility for checking trust in abilities
 * This ensures ALL abilities respect the trust system
 */
public class AbilityTrustValidator {

    private static final Map<UUID, Long> messageCooldowns = new HashMap<>();
    private static final long MESSAGE_COOLDOWN_MS = 3000; // 3 seconds

    /**
     * Check if an ability can affect a target based on trust
     *
     * @param plugin The plugin instance
     * @param caster The player using the ability
     * @param target The target entity
     * @param sendMessage Whether to send feedback message to caster
     * @return true if ability can affect target, false if trust prevents it
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

        // Check trust system
        if (plugin.getTrustManager().trusts(caster, targetPlayer)) {
            if (sendMessage && !isOnMessageCooldown(caster)) {
                caster.sendMessage(ChatColor.YELLOW + "Your ability cannot affect " +
                        targetPlayer.getName() + " - you trust them!");
                setMessageCooldown(caster);
            }
            return false;
        }

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
     *
     * @param plugin The plugin instance
     * @param caster The player using the ability
     * @param targets Collection of potential targets
     * @return List of valid targets (excluding trusted players)
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