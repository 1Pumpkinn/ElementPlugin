package saturn.elementPlugin.managers;

import saturn.elementPlugin.ElementPlugin;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

/**
 * Main TrustManager - now ONLY handles teams (individual trust removed)
 * Delegates everything to TeamManager
 */
public class TeamManager {
    private final ElementPlugin plugin;
    private final saturn.elementPlugin.managers.trust.TeamManager teamManager;

    public TeamManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = new saturn.elementPlugin.managers.trust.TeamManager(plugin);
    }

    // ========================================
    // MAIN TRUST CHECK - Used by all abilities
    // ========================================

    /**
     * Check if two players are trusted (team or ally relationship)
     * This is the MAIN method used by all element abilities
     */
    public boolean isTrusted(UUID player1, UUID player2) {
        // Same player is always trusted
        if (player1.equals(player2)) return true;

        // Check if they're on the same team (automatic trust)
        if (teamManager.areOnSameTeam(player1, player2)) return true;

        // Check if they're allies
        return teamManager.areAllies(player1, player2);
    }

    /**
     * Get list of team member names for display (backwards compatibility)
     */
    public List<String> getTrustedNames(UUID owner) {
        return teamManager.getTeamMemberNames(owner);
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
    // ALLY MANAGEMENT - New functionality
    // ========================================

    /**
     * Request to ally with another team
     */
    public boolean requestAlly(Player requester, String targetTeamName) {
        return teamManager.requestAlly(requester, targetTeamName);
    }

    /**
     * Accept an ally request
     */
    public boolean acceptAlly(Player accepter, String requestingTeamName) {
        return teamManager.acceptAlly(accepter, requestingTeamName);
    }

    /**
     * Remove ally relationship between teams
     */
    public boolean removeAlly(Player player, String allyTeamName) {
        return teamManager.removeAlly(player, allyTeamName);
    }

    /**
     * Get all allied teams for a player's team
     */
    public java.util.Set<String> getAlliedTeams(UUID playerUUID) {
        return teamManager.getAlliedTeams(playerUUID);
    }

    /**
     * Get pending ally requests for a player's team
     */
    public java.util.Set<String> getPendingAllyRequests(UUID playerUUID) {
        return teamManager.getPendingAllyRequests(playerUUID);
    }

    // ========================================
    // TEAM CUSTOMIZATION - Delegate to TeamManager
    // ========================================

    public boolean setTeamColor(Player leader, String teamName, String color) {
        return teamManager.setTeamColor(leader, teamName, color);
    }

    public boolean toggleTeamBold(Player leader, String teamName) {
        return teamManager.toggleTeamBold(leader, teamName);
    }

    public boolean toggleTeamItalic(Player leader, String teamName) {
        return teamManager.toggleTeamItalic(leader, teamName);
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