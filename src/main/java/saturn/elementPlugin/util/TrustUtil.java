package saturn.elementPlugin.util;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.managers.TrustManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

/**
 * Utility class for checking trust in abilities
 */
public class TrustUtil {

    /**
     * Check if a player should be able to affect a target with their ability
     * Returns true if the target should be affected
     * Returns false if the target is trusted and should not be affected
     *
     * @param plugin The plugin instance
     * @param caster The player casting the ability
     * @param target The target entity
     * @return true if target should be affected, false if trusted
     */
    public static boolean canAffect(ElementPlugin plugin, Player caster, LivingEntity target) {
        // Only check trust for player targets
        if (!(target instanceof Player targetPlayer)) {
            return true; // Can always affect non-players
        }

        // Can't affect yourself (handled elsewhere but double check)
        if (targetPlayer.equals(caster)) {
            return false;
        }

        // Check if caster trusts the target
        TrustManager trustManager = plugin.getTrustManager();
        if (trustManager.trusts(caster, targetPlayer)) {
            return false; // Caster trusts target, don't affect them
        }

        return true; // Can affect target
    }

    /**
     * Check if two players mutually trust each other
     *
     * @param plugin The plugin instance
     * @param player1 First player
     * @param player2 Second player
     * @return true if they mutually trust each other
     */
    public static boolean mutualTrust(ElementPlugin plugin, Player player1, Player player2) {
        TrustManager trustManager = plugin.getTrustManager();
        return trustManager.mutualTrust(player1, player2);
    }

    /**
     * Check if a player trusts another player
     *
     * @param plugin The plugin instance
     * @param truster The player who might trust
     * @param trustee The player who might be trusted
     * @return true if truster trusts trustee
     */
    public static boolean trusts(ElementPlugin plugin, Player truster, Player trustee) {
        TrustManager trustManager = plugin.getTrustManager();
        return trustManager.trusts(truster, trustee);
    }
}