package saturn.elementPlugin.managers;

import saturn.elementPlugin.ElementPlugin;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Main TeamManager - handles both teams AND individual trust
 * Individual trust is managed in-memory and persisted via DataStore
 * UPDATED: Simplified ally system (ONE ally per team)
 */
public class TeamManager {
    private final ElementPlugin plugin;
    private final saturn.elementPlugin.managers.trust.TeamManager teamManager;
    private final saturn.elementPlugin.managers.trust.IndividualTrustManager individualTrustManager;

    public TeamManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.teamManager = new saturn.elementPlugin.managers.trust.TeamManager(plugin);
        this.individualTrustManager = new saturn.elementPlugin.managers.trust.IndividualTrustManager(plugin);
    }

    // ========================================
    // MAIN TRUST CHECK - Used by all abilities
    // ========================================

    /**
     * Check if two players are trusted (team, ally, or individual trust relationship)
     * This is the MAIN method used by all element abilities
     */
    public boolean isTrusted(UUID player1, UUID player2) {
        // Same player is always trusted
        if (player1.equals(player2)) return true;

        // Check if they're on the same team (automatic trust)
        if (teamManager.areOnSameTeam(player1, player2)) return true;

        // Check if they're allies
        if (teamManager.areAllies(player1, player2)) return true;

        // Check individual mutual trust
        return individualTrustManager.areMutuallyTrusted(player1, player2);
    }

    /**
     * Get list of team member names AND individually trusted players for display
     */
    public List<String> getTrustedNames(UUID owner) {
        List<String> names = new ArrayList<>();

        // Add team members
        names.addAll(teamManager.getTeamMemberNames(owner));

        // Add individually trusted players
        names.addAll(individualTrustManager.getTrustedNames(owner));

        return names;
    }

    // ========================================
    // INDIVIDUAL TRUST MANAGEMENT
    // ========================================

    /**
     * Add a pending individual trust request
     */
    public void addPending(UUID target, UUID requestor) {
        individualTrustManager.addPending(target, requestor);
    }

    /**
     * Check if there's a pending request
     */
    public boolean hasPending(UUID target, UUID requestor) {
        return individualTrustManager.hasPending(target, requestor);
    }

    /**
     * Clear a pending request
     */
    public void clearPending(UUID target, UUID requestor) {
        individualTrustManager.clearPending(target, requestor);
    }

    /**
     * Add mutual trust between two players
     */
    public void addMutualTrust(UUID a, UUID b) {
        individualTrustManager.addMutualTrust(a, b);
    }

    /**
     * Remove mutual trust between two players
     */
    public void removeMutualTrust(UUID a, UUID b) {
        individualTrustManager.removeMutualTrust(a, b);
    }

    /**
     * Get individually trusted players (excluding team members)
     */
    public Set<UUID> getIndividuallyTrusted(UUID owner) {
        return individualTrustManager.getTrusted(owner);
    }

    /**
     * Check if player has hidden their team prefix
     */
    public boolean isTeamHidden(UUID playerUUID) {
        return teamManager.isTeamHidden(playerUUID);
    }

    /**
     * Toggle team prefix visibility for a player
     */
    public void toggleTeamHidden(UUID playerUUID) {
        teamManager.toggleTeamHidden(playerUUID);
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
    // ALLY MANAGEMENT - SIMPLIFIED (ONE ALLY)
    // ========================================

    /**
     * Add an ally team (ONE ally maximum, direct add)
     */
    public boolean addAlly(Player requester, String targetTeamName) {
        return teamManager.addAlly(requester, targetTeamName);
    }

    /**
     * Remove ally relationship between teams
     */
    public boolean removeAlly(Player player, String allyTeamName) {
        return teamManager.removeAlly(player, allyTeamName);
    }

    /**
     * Get the allied team for a player's team (only ONE)
     */
    public String getAllyTeam(UUID playerUUID) {
        return teamManager.getAllyTeam(playerUUID);
    }

    // ========================================
    // TEAM CUSTOMIZATION - Delegate to TeamManager
    // ========================================

    /**
     * Rename a team (NEW)
     */
    public boolean renameTeam(Player leader, String oldName, String newName) {
        return teamManager.renameTeam(leader, oldName, newName);
    }

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