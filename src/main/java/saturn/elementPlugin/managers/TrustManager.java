package saturn.elementPlugin.managers;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.DataStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced TrustManager with team support and tab list integration
 */
public class TrustManager {
    private final ElementPlugin plugin;
    private final DataStore store;
    private final Map<UUID, Set<UUID>> trusted = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> pending = new ConcurrentHashMap<>();

    // Team management
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>(); // player -> team name
    private final Map<String, Set<UUID>> teams = new ConcurrentHashMap<>(); // team name -> members
    private final Map<String, UUID> teamLeaders = new ConcurrentHashMap<>(); // team name -> leader UUID

    public TrustManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    // ========================================
    // TRUST METHODS
    // ========================================

    public Set<UUID> getTrusted(UUID owner) {
        return trusted.computeIfAbsent(owner, store::getTrusted);
    }

    /**
     * Check if two players are mutually trusted or on the same team
     */
    public boolean isTrusted(UUID owner, UUID other) {
        // Same player is always trusted
        if (owner.equals(other)) return true;

        // Check if they're on the same team
        if (areOnSameTeam(owner, other)) return true;

        // Check manual trust
        return getTrusted(owner).contains(other);
    }

    public void addTrust(UUID owner, UUID other) {
        Set<UUID> set = getTrusted(owner);
        set.add(other);
        store.setTrusted(owner, set);
    }

    public void addMutualTrust(UUID a, UUID b) {
        addTrust(a, b);
        addTrust(b, a);
    }

    public void addPending(UUID target, UUID requestor) {
        pending.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet()).add(requestor);
    }

    public boolean hasPending(UUID target, UUID requestor) {
        return pending.getOrDefault(target, Set.of()).contains(requestor);
    }

    public void clearPending(UUID target, UUID requestor) {
        Set<UUID> set = pending.get(target);
        if (set != null) set.remove(requestor);
    }

    public void removeTrust(UUID owner, UUID other) {
        Set<UUID> set = getTrusted(owner);
        set.remove(other);
        store.setTrusted(owner, set);
    }

    public void removeMutualTrust(UUID a, UUID b) {
        removeTrust(a, b);
        removeTrust(b, a);
    }

    public List<String> getTrustedNames(UUID owner) {
        List<String> out = new ArrayList<>();
        for (UUID u : getTrusted(owner)) {
            Player p = Bukkit.getPlayer(u);
            out.add(p != null ? p.getName() : u.toString());
        }
        return out;
    }

    // ========================================
    // TEAM METHODS
    // ========================================

    /**
     * Create a new team with the player as leader
     */
    public boolean createTeam(Player leader, String teamName) {
        // Validate team name
        if (teamName == null || teamName.isEmpty() || teamName.length() > 16) {
            return false;
        }

        // Check if team already exists
        if (teams.containsKey(teamName)) {
            return false;
        }

        // Check if player is already in a team
        if (playerTeams.containsKey(leader.getUniqueId())) {
            return false;
        }

        // Create team
        Set<UUID> members = ConcurrentHashMap.newKeySet();
        members.add(leader.getUniqueId());
        teams.put(teamName, members);
        teamLeaders.put(teamName, leader.getUniqueId());
        playerTeams.put(leader.getUniqueId(), teamName);

        // Update tab list
        updatePlayerTeamDisplay(leader);

        return true;
    }

    /**
     * Invite a player to join a team
     */
    public boolean inviteToTeam(Player inviter, Player target, String teamName) {
        // Verify inviter is team leader
        UUID leaderUUID = teamLeaders.get(teamName);
        if (leaderUUID == null || !leaderUUID.equals(inviter.getUniqueId())) {
            return false;
        }

        // Check if target is already in a team
        if (playerTeams.containsKey(target.getUniqueId())) {
            return false;
        }

        // Add to pending
        addPending(target.getUniqueId(), inviter.getUniqueId());

        return true;
    }

    /**
     * Accept a team invitation
     */
    public boolean acceptTeamInvite(Player player, String teamName) {
        // Check if player has pending invite
        UUID leaderUUID = teamLeaders.get(teamName);
        if (leaderUUID == null || !hasPending(player.getUniqueId(), leaderUUID)) {
            return false;
        }

        // Add to team
        Set<UUID> members = teams.get(teamName);
        if (members == null) return false;

        members.add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), teamName);
        clearPending(player.getUniqueId(), leaderUUID);

        // Update tab list
        updatePlayerTeamDisplay(player);

        return true;
    }

    /**
     * Leave current team
     */
    public boolean leaveTeam(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());
        if (teamName == null) return false;

        UUID leaderUUID = teamLeaders.get(teamName);
        Set<UUID> members = teams.get(teamName);

        // If player is leader, disband team
        if (leaderUUID != null && leaderUUID.equals(player.getUniqueId())) {
            return disbandTeam(teamName);
        }

        // Remove from team
        if (members != null) {
            members.remove(player.getUniqueId());
        }
        playerTeams.remove(player.getUniqueId());

        // Update tab list
        updatePlayerTeamDisplay(player);

        return true;
    }

    /**
     * Disband a team (leader only)
     */
    public boolean disbandTeam(String teamName) {
        Set<UUID> members = teams.get(teamName);
        if (members == null) return false;

        // Remove all members
        for (UUID memberUUID : members) {
            playerTeams.remove(memberUUID);
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                updatePlayerTeamDisplay(member);
            }
        }

        // Remove team
        teams.remove(teamName);
        teamLeaders.remove(teamName);

        return true;
    }

    /**
     * Kick a player from team (leader only)
     */
    public boolean kickFromTeam(Player leader, Player target, String teamName) {
        // Verify leader
        UUID leaderUUID = teamLeaders.get(teamName);
        if (leaderUUID == null || !leaderUUID.equals(leader.getUniqueId())) {
            return false;
        }

        // Can't kick yourself
        if (leader.equals(target)) return false;

        // Remove target
        Set<UUID> members = teams.get(teamName);
        if (members == null) return false;

        members.remove(target.getUniqueId());
        playerTeams.remove(target.getUniqueId());

        // Update tab list
        updatePlayerTeamDisplay(target);

        return true;
    }

    /**
     * Check if two players are on the same team
     */
    public boolean areOnSameTeam(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);

        return team1 != null && team1.equals(team2);
    }

    /**
     * Get player's team name
     */
    public String getPlayerTeam(UUID playerUUID) {
        return playerTeams.get(playerUUID);
    }

    /**
     * Get all members of a team
     */
    public Set<UUID> getTeamMembers(String teamName) {
        Set<UUID> members = teams.get(teamName);
        return members != null ? new HashSet<>(members) : new HashSet<>();
    }

    /**
     * Check if player is team leader
     */
    public boolean isTeamLeader(UUID playerUUID, String teamName) {
        UUID leaderUUID = teamLeaders.get(teamName);
        return leaderUUID != null && leaderUUID.equals(playerUUID);
    }

    // ========================================
    // TAB LIST INTEGRATION
    // ========================================

    /**
     * Update player's display in tab list with team prefix
     */
    private void updatePlayerTeamDisplay(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Remove from any existing team
        Team existingTeam = scoreboard.getPlayerTeam(player);
        if (existingTeam != null) {
            existingTeam.removeEntry(player.getName());
        }

        if (teamName != null) {
            // Get or create scoreboard team
            Team scoreboardTeam = scoreboard.getTeam(teamName);
            if (scoreboardTeam == null) {
                scoreboardTeam = scoreboard.registerNewTeam(teamName);
                scoreboardTeam.prefix(Component.text("[" + teamName + "] ").color(NamedTextColor.AQUA));
                scoreboardTeam.setAllowFriendlyFire(false); // Prevent team damage
            }

            // Add player to team
            scoreboardTeam.addEntry(player.getName());
        }
    }

    /**
     * Update all online players' tab list displays
     */
    public void updateAllTabLists() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTeamDisplay(player);
        }
    }

    /**
     * Clear all team data (for plugin reload)
     */
    public void clearAllTeams() {
        teams.clear();
        playerTeams.clear();
        teamLeaders.clear();

        // Clear scoreboard teams
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
    }
}