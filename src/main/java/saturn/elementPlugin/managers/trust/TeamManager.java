package saturn.elementPlugin.managers.trust;

import saturn.elementPlugin.ElementPlugin;
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
 * Manages team creation, invites, and tab list display
 */
public class TeamManager {
    private final ElementPlugin plugin;

    // Team data structures
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> teams = new ConcurrentHashMap<>();
    private final Map<String, UUID> teamLeaders = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> teamInvites = new ConcurrentHashMap<>();

    // Tab list colors
    private static final NamedTextColor[] TEAM_COLORS = {
            NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.YELLOW,
            NamedTextColor.LIGHT_PURPLE, NamedTextColor.GOLD, NamedTextColor.RED,
            NamedTextColor.DARK_AQUA, NamedTextColor.DARK_GREEN, NamedTextColor.BLUE
    };
    private final Map<String, NamedTextColor> teamColors = new ConcurrentHashMap<>();
    private int colorIndex = 0;

    public TeamManager(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    // ========================================
    // TEAM CHECKS
    // ========================================

    public boolean areOnSameTeam(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);
        return team1 != null && team1.equals(team2);
    }

    // ========================================
    // TEAM CREATION & MANAGEMENT
    // ========================================

    public boolean createTeam(Player leader, String teamName) {
        // Validate team name
        if (!isValidTeamName(teamName)) {
            leader.sendMessage(Component.text("Invalid team name! Must be 1-16 characters, no spaces.", NamedTextColor.RED));
            return false;
        }

        // Check if team exists
        if (teams.containsKey(teamName)) {
            leader.sendMessage(Component.text("A team with that name already exists!", NamedTextColor.RED));
            return false;
        }

        // Check if player is already in a team
        if (playerTeams.containsKey(leader.getUniqueId())) {
            leader.sendMessage(Component.text("You are already in a team!", NamedTextColor.RED));
            return false;
        }

        // Create team
        Set<UUID> members = ConcurrentHashMap.newKeySet();
        members.add(leader.getUniqueId());
        teams.put(teamName, members);
        teamLeaders.put(teamName, leader.getUniqueId());
        playerTeams.put(leader.getUniqueId(), teamName);

        // Assign color
        assignTeamColor(teamName);

        // Create scoreboard team and update tab list
        createScoreboardTeam(teamName);
        updatePlayerTabList(leader);

        plugin.getLogger().info("Team '" + teamName + "' created by " + leader.getName());
        return true;
    }

    public boolean inviteToTeam(Player inviter, Player target, String teamName) {
        // Verify inviter is team leader
        if (!isTeamLeader(inviter.getUniqueId(), teamName)) {
            inviter.sendMessage(Component.text("You must be the team leader to invite players!", NamedTextColor.RED));
            return false;
        }

        // Check if target is already in a team
        if (playerTeams.containsKey(target.getUniqueId())) {
            inviter.sendMessage(Component.text(target.getName() + " is already in a team!", NamedTextColor.RED));
            return false;
        }

        // Add to pending invites
        teamInvites.computeIfAbsent(target.getUniqueId(), k -> ConcurrentHashMap.newKeySet())
                .add(teamName);

        plugin.getLogger().info(inviter.getName() + " invited " + target.getName() + " to team '" + teamName + "'");
        return true;
    }

    public boolean acceptTeamInvite(Player player, String teamName) {
        // Check if player has pending invite
        Set<String> invites = teamInvites.get(player.getUniqueId());
        if (invites == null || !invites.contains(teamName)) {
            player.sendMessage(Component.text("You don't have an invite from that team!", NamedTextColor.RED));
            return false;
        }

        // Check if team still exists
        Set<UUID> members = teams.get(teamName);
        if (members == null) {
            invites.remove(teamName);
            player.sendMessage(Component.text("That team no longer exists!", NamedTextColor.RED));
            return false;
        }

        // Check if player is already in another team
        if (playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a team!", NamedTextColor.RED));
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

    public boolean leaveTeam(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());
        if (teamName == null) {
            player.sendMessage(Component.text("You are not in a team!", NamedTextColor.RED));
            return false;
        }

        // If player is leader, disband team
        if (isTeamLeader(player.getUniqueId(), teamName)) {
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

        // Remove team data
        teams.remove(teamName);
        teamLeaders.remove(teamName);
        teamColors.remove(teamName);

        // Remove scoreboard team
        removeScoreboardTeam(teamName);

        return true;
    }

    public boolean kickFromTeam(Player leader, Player target, String teamName) {
        // Verify leader
        if (!isTeamLeader(leader.getUniqueId(), teamName)) {
            leader.sendMessage(Component.text("You must be the team leader to kick players!", NamedTextColor.RED));
            return false;
        }

        // Can't kick yourself
        if (leader.equals(target)) {
            leader.sendMessage(Component.text("You cannot kick yourself! Use /trust team leave instead.", NamedTextColor.RED));
            return false;
        }

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

    // ========================================
    // TEAM QUERIES
    // ========================================

    public String getPlayerTeam(UUID playerUUID) {
        return playerTeams.get(playerUUID);
    }

    public Set<UUID> getTeamMembers(String teamName) {
        Set<UUID> members = teams.get(teamName);
        return members != null ? new HashSet<>(members) : new HashSet<>();
    }

    public boolean isTeamLeader(UUID playerUUID, String teamName) {
        UUID leaderUUID = teamLeaders.get(teamName);
        return leaderUUID != null && leaderUUID.equals(playerUUID);
    }

    public Set<String> getPendingInvites(UUID playerUUID) {
        Set<String> invites = teamInvites.get(playerUUID);
        return invites != null ? new HashSet<>(invites) : new HashSet<>();
    }

    // ========================================
    // TAB LIST MANAGEMENT
    // ========================================

    /**
     * Update player's display in tab list with team prefix
     * FIXED: Now properly shows team name beside username
     */
    public void updatePlayerTabList(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());

        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Remove from any existing team
        Team existingTeam = scoreboard.getPlayerTeam(player);
        if (existingTeam != null) {
            existingTeam.removeEntry(player.getName());
        }

        if (teamName != null) {
            // Get or create scoreboard team
            Team scoreboardTeam = getOrCreateScoreboardTeam(teamName);

            // Add player to team (this automatically adds the prefix)
            scoreboardTeam.addEntry(player.getName());

            plugin.getLogger().fine("Added " + player.getName() + " to scoreboard team '" + teamName + "'");
        }
    }

    private Team getOrCreateScoreboardTeam(String teamName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team scoreboardTeam = scoreboard.getTeam(teamName);

        if (scoreboardTeam == null) {
            scoreboardTeam = createScoreboardTeam(teamName);
        }

        return scoreboardTeam;
    }

    private Team createScoreboardTeam(String teamName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Remove existing team if it exists
        Team existing = scoreboard.getTeam(teamName);
        if (existing != null) {
            existing.unregister();
        }

        Team scoreboardTeam = scoreboard.registerNewTeam(teamName);

        // Get team color
        NamedTextColor color = teamColors.getOrDefault(teamName, NamedTextColor.WHITE);

        // FIXED: Set prefix with team name - this shows beside username in tab list
        scoreboardTeam.prefix(Component.text("[" + teamName + "] ")
                .color(color)
                .decoration(TextDecoration.BOLD, true));

        // Set team color for name
        scoreboardTeam.color(color);

        // Disable friendly fire
        scoreboardTeam.setAllowFriendlyFire(false);

        // Enable see invisible teammates
        scoreboardTeam.setCanSeeFriendlyInvisibles(true);

        plugin.getLogger().info("Created scoreboard team '" + teamName + "' with prefix");

        return scoreboardTeam;
    }

    private void removeScoreboardTeam(String teamName) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team scoreboardTeam = scoreboard.getTeam(teamName);
        if (scoreboardTeam != null) {
            scoreboardTeam.unregister();
        }
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    private void assignTeamColor(String teamName) {
        NamedTextColor color = TEAM_COLORS[colorIndex % TEAM_COLORS.length];
        teamColors.put(teamName, color);
        colorIndex++;
    }

    private boolean isValidTeamName(String teamName) {
        return teamName != null
                && !teamName.isEmpty()
                && teamName.length() <= 16
                && !teamName.contains(" ");
    }

    public void handlePlayerQuit(UUID playerUUID) {
        // Clean up pending invites TO them
        teamInvites.remove(playerUUID);
    }
}