package saturn.elementPlugin.managers;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.DataStore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced TrustManager with team support and tab list integration
 * Teams provide automatic trust between all members
 * Individual trust still works independently of teams
 */
public class TrustManager {
    private final ElementPlugin plugin;
    private final DataStore store;

    // Individual trust relationships
    private final Map<UUID, Set<UUID>> trusted = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> pending = new ConcurrentHashMap<>();

    // Team management
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>(); // player -> team name
    private final Map<String, Set<UUID>> teams = new ConcurrentHashMap<>(); // team name -> members
    private final Map<String, UUID> teamLeaders = new ConcurrentHashMap<>(); // team name -> leader UUID
    private final Map<UUID, Set<String>> teamInvites = new ConcurrentHashMap<>(); // player -> pending team invites

    // Tab list colors for teams
    private static final NamedTextColor[] TEAM_COLORS = {
            NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.YELLOW,
            NamedTextColor.LIGHT_PURPLE, NamedTextColor.GOLD, NamedTextColor.RED
    };
    private final Map<String, NamedTextColor> teamColors = new ConcurrentHashMap<>();
    private int colorIndex = 0;

    public TrustManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    // ========================================
    // CORE TRUST CHECK - Used by all abilities
    // ========================================

    /**
     * Check if two players are trusted (either through team or individual trust)
     * This is the MAIN method used by all element abilities
     */
    public boolean isTrusted(UUID player1, UUID player2) {
        // Same player is always trusted
        if (player1.equals(player2)) return true;

        // Check if they're on the same team (automatic trust)
        if (areOnSameTeam(player1, player2)) return true;

        // Check individual mutual trust
        return getTrusted(player1).contains(player2) &&
                getTrusted(player2).contains(player1);
    }

    /**
     * Check if two players are on the same team
     */
    public boolean areOnSameTeam(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);

        return team1 != null && team1.equals(team2);
    }

    // ========================================
    // INDIVIDUAL TRUST METHODS
    // ========================================

    public Set<UUID> getTrusted(UUID owner) {
        return trusted.computeIfAbsent(owner, store::getTrusted);
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

    public void removeTrust(UUID owner, UUID other) {
        Set<UUID> set = getTrusted(owner);
        set.remove(other);
        store.setTrusted(owner, set);
    }

    public void removeMutualTrust(UUID a, UUID b) {
        removeTrust(a, b);
        removeTrust(b, a);
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

    public List<String> getTrustedNames(UUID owner) {
        List<String> out = new ArrayList<>();
        for (UUID u : getTrusted(owner)) {
            Player p = Bukkit.getPlayer(u);
            out.add(p != null ? p.getName() : u.toString());
        }
        return out;
    }

    // ========================================
    // TEAM MANAGEMENT
    // ========================================

    /**
     * Create a new team with the player as leader
     */
    public boolean createTeam(Player leader, String teamName) {
        // Validate team name (1-16 chars, no spaces)
        if (teamName == null || teamName.isEmpty() || teamName.length() > 16) {
            return false;
        }
        if (teamName.contains(" ")) {
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

        // Assign color to team
        assignTeamColor(teamName);

        // Update tab list
        updatePlayerTabList(leader);

        plugin.getLogger().info("Team '" + teamName + "' created by " + leader.getName());

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

        // Add to pending invites
        teamInvites.computeIfAbsent(target.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(teamName);

        plugin.getLogger().info(inviter.getName() + " invited " + target.getName() + " to team '" + teamName + "'");

        return true;
    }

    /**
     * Accept a team invitation
     */
    public boolean acceptTeamInvite(Player player, String teamName) {
        // Check if player has pending invite
        Set<String> invites = teamInvites.get(player.getUniqueId());
        if (invites == null || !invites.contains(teamName)) {
            return false;
        }

        // Check if team still exists
        Set<UUID> members = teams.get(teamName);
        if (members == null) {
            // Team was disbanded, remove invite
            invites.remove(teamName);
            return false;
        }

        // Check if player is already in another team
        if (playerTeams.containsKey(player.getUniqueId())) {
            return false;
        }

        // Add to team
        members.add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), teamName);
        invites.remove(teamName);

        // Update tab list
        updatePlayerTabList(player);

        plugin.getLogger().info(player.getName() + " joined team '" + teamName + "'");

        return true;
    }

    /**
     * Leave current team
     */
    public boolean leaveTeam(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());
        if (teamName == null) return false;

        UUID leaderUUID = teamLeaders.get(teamName);

        // If player is leader, disband team
        if (leaderUUID != null && leaderUUID.equals(player.getUniqueId())) {
            disbandTeam(teamName);
            return true;
        }

        // Remove from team
        Set<UUID> members = teams.get(teamName);
        if (members != null) {
            members.remove(player.getUniqueId());
        }
        playerTeams.remove(player.getUniqueId());

        // Update tab list
        updatePlayerTabList(player);

        plugin.getLogger().info(player.getName() + " left team '" + teamName + "'");

        return true;
    }

    /**
     * Disband a team (leader only)
     */
    public boolean disbandTeam(String teamName) {
        Set<UUID> members = teams.get(teamName);
        if (members == null) return false;

        plugin.getLogger().info("Disbanding team '" + teamName + "'");

        // Remove all members
        for (UUID memberUUID : new ArrayList<>(members)) {
            playerTeams.remove(memberUUID);
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                updatePlayerTabList(member);
            }
        }

        // Remove team
        teams.remove(teamName);
        teamLeaders.remove(teamName);
        teamColors.remove(teamName);

        // Remove scoreboard team
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team scoreboardTeam = scoreboard.getTeam(teamName);
        if (scoreboardTeam != null) {
            scoreboardTeam.unregister();
        }

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
        updatePlayerTabList(target);

        plugin.getLogger().info(leader.getName() + " kicked " + target.getName() + " from team '" + teamName + "'");

        return true;
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

    /**
     * Get pending team invites for a player
     */
    public Set<String> getPendingInvites(UUID playerUUID) {
        Set<String> invites = teamInvites.get(playerUUID);
        return invites != null ? new HashSet<>(invites) : new HashSet<>();
    }

    // ========================================
    // TAB LIST INTEGRATION
    // ========================================

    /**
     * Assign a color to a team
     */
    private void assignTeamColor(String teamName) {
        NamedTextColor color = TEAM_COLORS[colorIndex % TEAM_COLORS.length];
        teamColors.put(teamName, color);
        colorIndex++;
    }

    /**
     * Update player's display in tab list with team prefix
     */
    private void updatePlayerTabList(Player player) {
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

                // Get team color
                NamedTextColor color = teamColors.getOrDefault(teamName, NamedTextColor.WHITE);

                // Set prefix with team name and color
                scoreboardTeam.prefix(Component.text("[" + teamName + "] ")
                        .color(color)
                        .decoration(TextDecoration.BOLD, true));

                // Set team color
                scoreboardTeam.color(color);

                // Disable friendly fire
                scoreboardTeam.setAllowFriendlyFire(false);

                // Enable see invisible teammates
                scoreboardTeam.setCanSeeFriendlyInvisibles(true);
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
            updatePlayerTabList(player);
        }
    }

    /**
     * Clear all team data (for plugin reload)
     */
    public void clearAllTeams() {
        teams.clear();
        playerTeams.clear();
        teamLeaders.clear();
        teamInvites.clear();
        teamColors.clear();

        // Clear scoreboard teams
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            team.unregister();
        }
    }

    /**
     * Handle player disconnect - cleanup
     */
    public void handlePlayerQuit(UUID playerUUID) {
        // Don't remove from team, they can rejoin
        // Just clean up any pending invites TO them
        teamInvites.remove(playerUUID);
    }

    /**
     * Handle player join - restore tab list
     */
    public void handlePlayerJoin(Player player) {
        // Restore tab list display if in team
        if (playerTeams.containsKey(player.getUniqueId())) {
            updatePlayerTabList(player);
        }
    }
}