package saturn.elementPlugin.managers;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.managers.trust.IndividualTrustManager;
import saturn.elementPlugin.managers.trust.TeamManager;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Main TrustManager - delegates to specialized managers
 * This is the main API that other classes use
 */
public class TrustManager {
    private final ElementPlugin plugin;
    private final IndividualTrustManager individualTrust;
    private final TeamManager teamManager;

    public TrustManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.individualTrust = new IndividualTrustManager(plugin);
        this.teamManager = new TeamManager(plugin);
    }

    // ========================================
    // MAIN TRUST CHECK - Used by all abilities
    // ========================================

    /**
     * Check if two players are trusted (either through team or individual trust)
     * This is the MAIN method used by all element abilities
     */
    public boolean isTrusted(UUID player1, UUID player2) {
        // Same player is always trusted
        if (player1.equals(player2)) return true;

        // Check if they're on the same team (automatic trust)
        if (teamManager.areOnSameTeam(player1, player2)) return true;

        // Check individual mutual trust
        return individualTrust.areMutuallyTrusted(player1, player2);
    }

    // ========================================
    // INDIVIDUAL TRUST - Delegate to IndividualTrustManager
    // ========================================

    public java.util.Set<UUID> getTrusted(UUID owner) {
        return individualTrust.getTrusted(owner);
    }

    public void addMutualTrust(UUID a, UUID b) {
        individualTrust.addMutualTrust(a, b);
    }

    public void removeMutualTrust(UUID a, UUID b) {
        individualTrust.removeMutualTrust(a, b);
    }

    public void addPending(UUID target, UUID requestor) {
        individualTrust.addPending(target, requestor);
    }

    public boolean hasPending(UUID target, UUID requestor) {
        return individualTrust.hasPending(target, requestor);
    }

    public void clearPending(UUID target, UUID requestor) {
        individualTrust.clearPending(target, requestor);
    }

    public java.util.List<String> getTrustedNames(UUID owner) {
        return individualTrust.getTrustedNames(owner);
    }

    // ========================================
    // TEAM MANAGEMENT - Delegate to TeamManager
    // ========================================

    public boolean createTeam(Player leader, String teamName) {
        return teamManager.createTeam(leader, teamName);
    }

    public boolean inviteToTeam(Player inviter, Player target, String teamName) {
        return teamManager.inviteToTeam(inviter, target, teamName);
    }

    public boolean acceptTeamInvite(Player player, String teamName) {
        return teamManager.acceptTeamInvite(player, teamName);
    }

    public boolean leaveTeam(Player player) {
        return teamManager.leaveTeam(player);
    }

    public boolean disbandTeam(String teamName) {
        return teamManager.disbandTeam(teamName);
    }

    public boolean kickFromTeam(Player leader, Player target, String teamName) {
        return teamManager.kickFromTeam(leader, target, teamName);
    }

    public String getPlayerTeam(UUID playerUUID) {
        return teamManager.getPlayerTeam(playerUUID);
    }

    public java.util.Set<UUID> getTeamMembers(String teamName) {
        return teamManager.getTeamMembers(teamName);
    }

    public boolean isTeamLeader(UUID playerUUID, String teamName) {
        return teamManager.isTeamLeader(playerUUID, teamName);
    }

    public java.util.Set<String> getPendingInvites(UUID playerUUID) {
        return teamManager.getPendingInvites(playerUUID);
    }

    // ========================================
    // LIFECYCLE METHODS
    // ========================================

    public void handlePlayerJoin(Player player) {
        teamManager.updatePlayerTabList(player);
    }

    public void handlePlayerQuit(UUID playerUUID) {
        teamManager.handlePlayerQuit(playerUUID);
    }
}