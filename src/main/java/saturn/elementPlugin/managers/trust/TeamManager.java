package saturn.elementPlugin.managers.trust;

import saturn.elementPlugin.ElementPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages team creation, invites, and tab list display with customization
 */
public class TeamManager {
    private final ElementPlugin plugin;

    // Team data structures
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> teams = new ConcurrentHashMap<>();
    private final Map<String, UUID> teamLeaders = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> teamInvites = new ConcurrentHashMap<>();

    // Team customization
    private final Map<String, TextColor> teamColors = new ConcurrentHashMap<>();
    private final Map<String, Boolean> teamBold = new ConcurrentHashMap<>();
    private final Map<String, Boolean> teamItalic = new ConcurrentHashMap<>();

    // Default colors for new teams
    private static final NamedTextColor[] DEFAULT_COLORS = {
            NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.YELLOW,
            NamedTextColor.LIGHT_PURPLE, NamedTextColor.GOLD, NamedTextColor.RED,
            NamedTextColor.DARK_AQUA, NamedTextColor.DARK_GREEN, NamedTextColor.BLUE
    };
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

        // Assign default color and formatting
        assignDefaultFormatting(teamName);

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
        teamBold.remove(teamName);
        teamItalic.remove(teamName);

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
    // TEAM CUSTOMIZATION
    // ========================================

    /**
     * Set team color
     * @param leader Team leader
     * @param teamName Team name
     * @param color Color (hex or named color)
     * @return true if successful
     */
    public boolean setTeamColor(Player leader, String teamName, String color) {
        if (!isTeamLeader(leader.getUniqueId(), teamName)) {
            leader.sendMessage(Component.text("Only the team leader can change the team color!", NamedTextColor.RED));
            return false;
        }

        TextColor textColor = parseColor(color);
        if (textColor == null) {
            leader.sendMessage(Component.text("Invalid color! Use a color name (red, blue, etc.) or hex (#FF5733)", NamedTextColor.RED));
            return false;
        }

        teamColors.put(teamName, textColor);
        refreshTeamDisplay(teamName);

        leader.sendMessage(Component.text("Team color updated to ", NamedTextColor.GREEN)
                .append(Component.text(color, textColor)));
        return true;
    }

    /**
     * Toggle team name bold formatting
     */
    public boolean toggleTeamBold(Player leader, String teamName) {
        if (!isTeamLeader(leader.getUniqueId(), teamName)) {
            leader.sendMessage(Component.text("Only the team leader can change formatting!", NamedTextColor.RED));
            return false;
        }

        boolean currentBold = teamBold.getOrDefault(teamName, false);
        teamBold.put(teamName, !currentBold);
        refreshTeamDisplay(teamName);

        leader.sendMessage(Component.text("Team bold formatting " + (!currentBold ? "enabled" : "disabled"), NamedTextColor.GREEN));
        return true;
    }

    /**
     * Toggle team name italic formatting
     */
    public boolean toggleTeamItalic(Player leader, String teamName) {
        if (!isTeamLeader(leader.getUniqueId(), teamName)) {
            leader.sendMessage(Component.text("Only the team leader can change formatting!", NamedTextColor.RED));
            return false;
        }

        boolean currentItalic = teamItalic.getOrDefault(teamName, false);
        teamItalic.put(teamName, !currentItalic);
        refreshTeamDisplay(teamName);

        leader.sendMessage(Component.text("Team italic formatting " + (!currentItalic ? "enabled" : "disabled"), NamedTextColor.GREEN));
        return true;
    }

    /**
     * Refresh tab list display for all team members
     */
    private void refreshTeamDisplay(String teamName) {
        // Recreate scoreboard team with new formatting
        removeScoreboardTeam(teamName);
        createScoreboardTeam(teamName);

        // Update all team members
        Set<UUID> members = teams.get(teamName);
        if (members != null) {
            for (UUID memberUUID : members) {
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null) {
                    updatePlayerTabList(member);
                }
            }
        }
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

    /**
     * Get team color
     */
    public TextColor getTeamColor(String teamName) {
        return teamColors.get(teamName);
    }

    /**
     * Check if team is bold
     */
    public boolean isTeamBold(String teamName) {
        return teamBold.getOrDefault(teamName, false);
    }

    /**
     * Check if team is italic
     */
    public boolean isTeamItalic(String teamName) {
        return teamItalic.getOrDefault(teamName, false);
    }

    // ========================================
    // TAB LIST MANAGEMENT
    // ========================================

    /**
     * Update player's display in tab list with team prefix
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

        // Get team formatting
        TextColor color = teamColors.getOrDefault(teamName, NamedTextColor.WHITE);
        boolean bold = teamBold.getOrDefault(teamName, false);
        boolean italic = teamItalic.getOrDefault(teamName, false);

        // Create formatted prefix with cleaner design: [TeamName]
        Component prefix = Component.text("[" + teamName + "] ", color);

        if (bold) {
            prefix = prefix.decoration(TextDecoration.BOLD, true);
        }
        if (italic) {
            prefix = prefix.decoration(TextDecoration.ITALIC, true);
        }

        scoreboardTeam.prefix(prefix);

        // Set team color for player names (converts NamedTextColor or uses white)
        if (color instanceof NamedTextColor namedColor) {
            scoreboardTeam.color(namedColor);
        } else {
            // Hex colors can't be set as team color in scoreboard, use white
            scoreboardTeam.color(NamedTextColor.WHITE);
        }

        // Disable friendly fire
        scoreboardTeam.setAllowFriendlyFire(false);

        // Enable see invisible teammates
        scoreboardTeam.setCanSeeFriendlyInvisibles(true);

        plugin.getLogger().info("Created scoreboard team '" + teamName + "' with custom formatting");

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

    private void assignDefaultFormatting(String teamName) {
        NamedTextColor color = DEFAULT_COLORS[colorIndex % DEFAULT_COLORS.length];
        teamColors.put(teamName, color);
        teamBold.put(teamName, false);
        teamItalic.put(teamName, false);
        colorIndex++;
    }

    private TextColor parseColor(String colorStr) {
        // Try hex color first
        if (colorStr.startsWith("#")) {
            try {
                return TextColor.fromHexString(colorStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        // Try named color
        try {
            return NamedTextColor.NAMES.value(colorStr.toLowerCase());
        } catch (Exception e) {
            return null;
        }
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