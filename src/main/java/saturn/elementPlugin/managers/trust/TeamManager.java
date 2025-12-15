package saturn.elementPlugin.managers.trust;

import saturn.elementPlugin.ElementPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages team creation, invites, allies, and tab list display with customization
 * UPDATED: Added ally system
 */
public class TeamManager {
    private final ElementPlugin plugin;

    // Team data structures
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> teams = new ConcurrentHashMap<>();
    private final Map<String, UUID> teamLeaders = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> teamInvites = new ConcurrentHashMap<>();

    // Ally system - NEW
    private final Map<String, Set<String>> teamAllies = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> pendingAllyRequests = new ConcurrentHashMap<>();

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

    /**
     * Check if two players are allies (their teams are allied)
     */
    public boolean areAllies(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);

        if (team1 == null || team2 == null || team1.equals(team2)) {
            return false; // Same team or no team
        }

        Set<String> allies1 = teamAllies.get(team1);
        return allies1 != null && allies1.contains(team2);
    }

    // ========================================
    // ALLY SYSTEM - NEW
    // ========================================

    /**
     * Request to ally with another team
     */
    public boolean requestAlly(Player requester, String targetTeamName) {
        String requesterTeam = playerTeams.get(requester.getUniqueId());

        // Check if requester is in a team
        if (requesterTeam == null) {
            requester.sendMessage(Component.text("You must be in a team to ally with others!", NamedTextColor.RED));
            return false;
        }

        // Check if requester is team leader
        if (!isTeamLeader(requester.getUniqueId(), requesterTeam)) {
            requester.sendMessage(Component.text("Only the team leader can send ally requests!", NamedTextColor.RED));
            return false;
        }

        // Check if target team exists
        if (!teams.containsKey(targetTeamName)) {
            requester.sendMessage(Component.text("Team '" + targetTeamName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        // Can't ally with yourself
        if (requesterTeam.equals(targetTeamName)) {
            requester.sendMessage(Component.text("You cannot ally with your own team!", NamedTextColor.RED));
            return false;
        }

        // Check if already allies
        Set<String> currentAllies = teamAllies.get(requesterTeam);
        if (currentAllies != null && currentAllies.contains(targetTeamName)) {
            requester.sendMessage(Component.text("Your team is already allied with " + targetTeamName + "!", NamedTextColor.YELLOW));
            return false;
        }

        // Add to pending requests
        pendingAllyRequests.computeIfAbsent(targetTeamName, k -> ConcurrentHashMap.newKeySet())
                .add(requesterTeam);

        // Notify target team leader
        UUID targetLeaderUUID = teamLeaders.get(targetTeamName);
        Player targetLeader = Bukkit.getPlayer(targetLeaderUUID);
        if (targetLeader != null) {
            targetLeader.sendMessage(Component.text("Team ", NamedTextColor.GOLD)
                    .append(Component.text(requesterTeam, NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(" wants to ally with your team!", NamedTextColor.GOLD)));
            targetLeader.sendMessage(Component.text("Use ", NamedTextColor.GRAY)
                    .append(Component.text("/team ally accept " + requesterTeam, NamedTextColor.AQUA))
                    .append(Component.text(" to accept", NamedTextColor.GRAY)));
        }

        requester.sendMessage(Component.text("✓ Sent ally request to team " + targetTeamName, NamedTextColor.GREEN));
        plugin.getLogger().info("Team '" + requesterTeam + "' requested ally with '" + targetTeamName + "'");
        return true;
    }

    /**
     * Accept an ally request
     */
    public boolean acceptAlly(Player accepter, String requestingTeamName) {
        String accepterTeam = playerTeams.get(accepter.getUniqueId());

        // Check if accepter is in a team
        if (accepterTeam == null) {
            accepter.sendMessage(Component.text("You must be in a team!", NamedTextColor.RED));
            return false;
        }

        // Check if accepter is team leader
        if (!isTeamLeader(accepter.getUniqueId(), accepterTeam)) {
            accepter.sendMessage(Component.text("Only the team leader can accept ally requests!", NamedTextColor.RED));
            return false;
        }

        // Check if there's a pending request
        Set<String> pending = pendingAllyRequests.get(accepterTeam);
        if (pending == null || !pending.contains(requestingTeamName)) {
            accepter.sendMessage(Component.text("No pending ally request from team " + requestingTeamName, NamedTextColor.RED));
            return false;
        }

        // Check if requesting team still exists
        if (!teams.containsKey(requestingTeamName)) {
            pending.remove(requestingTeamName);
            accepter.sendMessage(Component.text("That team no longer exists!", NamedTextColor.RED));
            return false;
        }

        // Create mutual ally relationship
        teamAllies.computeIfAbsent(accepterTeam, k -> ConcurrentHashMap.newKeySet())
                .add(requestingTeamName);
        teamAllies.computeIfAbsent(requestingTeamName, k -> ConcurrentHashMap.newKeySet())
                .add(accepterTeam);

        // Remove from pending
        pending.remove(requestingTeamName);

        // Notify both teams
        accepter.sendMessage(Component.text("✓ Your team is now allied with ", NamedTextColor.GREEN)
                .append(Component.text(requestingTeamName, NamedTextColor.AQUA, TextDecoration.BOLD)));

        UUID requestingLeaderUUID = teamLeaders.get(requestingTeamName);
        Player requestingLeader = Bukkit.getPlayer(requestingLeaderUUID);
        if (requestingLeader != null) {
            requestingLeader.sendMessage(Component.text("✓ Your ally request was accepted by team ", NamedTextColor.GREEN)
                    .append(Component.text(accepterTeam, NamedTextColor.AQUA, TextDecoration.BOLD)));
        }

        plugin.getLogger().info("Teams '" + accepterTeam + "' and '" + requestingTeamName + "' are now allies");
        return true;
    }

    /**
     * Remove ally relationship
     */
    public boolean removeAlly(Player player, String allyTeamName) {
        String playerTeam = playerTeams.get(player.getUniqueId());

        // Check if player is in a team
        if (playerTeam == null) {
            player.sendMessage(Component.text("You must be in a team!", NamedTextColor.RED));
            return false;
        }

        // Check if player is team leader
        if (!isTeamLeader(player.getUniqueId(), playerTeam)) {
            player.sendMessage(Component.text("Only the team leader can remove allies!", NamedTextColor.RED));
            return false;
        }

        // Check if they're actually allies
        Set<String> allies = teamAllies.get(playerTeam);
        if (allies == null || !allies.contains(allyTeamName)) {
            player.sendMessage(Component.text("Your team is not allied with " + allyTeamName, NamedTextColor.RED));
            return false;
        }

        // Remove mutual ally relationship
        allies.remove(allyTeamName);
        Set<String> otherAllies = teamAllies.get(allyTeamName);
        if (otherAllies != null) {
            otherAllies.remove(playerTeam);
        }

        // Notify both teams
        player.sendMessage(Component.text("✓ Removed ally relationship with team " + allyTeamName, NamedTextColor.YELLOW));

        UUID otherLeaderUUID = teamLeaders.get(allyTeamName);
        Player otherLeader = Bukkit.getPlayer(otherLeaderUUID);
        if (otherLeader != null) {
            otherLeader.sendMessage(Component.text("Team ", NamedTextColor.YELLOW)
                    .append(Component.text(playerTeam, NamedTextColor.AQUA))
                    .append(Component.text(" removed the ally relationship", NamedTextColor.YELLOW)));
        }

        plugin.getLogger().info("Ally relationship removed between '" + playerTeam + "' and '" + allyTeamName + "'");
        return true;
    }

    /**
     * Get all allied teams for a player's team
     */
    public Set<String> getAlliedTeams(UUID playerUUID) {
        String team = playerTeams.get(playerUUID);
        if (team == null) return new HashSet<>();

        Set<String> allies = teamAllies.get(team);
        return allies != null ? new HashSet<>(allies) : new HashSet<>();
    }

    /**
     * Get pending ally requests for a player's team
     */
    public Set<String> getPendingAllyRequests(UUID playerUUID) {
        String team = playerTeams.get(playerUUID);
        if (team == null) return new HashSet<>();

        Set<String> pending = pendingAllyRequests.get(team);
        return pending != null ? new HashSet<>(pending) : new HashSet<>();
    }

    // ========================================
    // TEAM CREATION & MANAGEMENT (existing code continues...)
    // ========================================

    public boolean createTeam(Player leader, String teamName) {
        if (!isValidTeamName(teamName)) {
            leader.sendMessage(Component.text("Invalid team name! Must be 1-16 characters, no spaces.", NamedTextColor.RED));
            return false;
        }

        if (teams.containsKey(teamName)) {
            leader.sendMessage(Component.text("A team with that name already exists!", NamedTextColor.RED));
            return false;
        }

        if (playerTeams.containsKey(leader.getUniqueId())) {
            leader.sendMessage(Component.text("You are already in a team!", NamedTextColor.RED));
            return false;
        }

        Set<UUID> members = ConcurrentHashMap.newKeySet();
        members.add(leader.getUniqueId());
        teams.put(teamName, members);
        teamLeaders.put(teamName, leader.getUniqueId());
        playerTeams.put(leader.getUniqueId(), teamName);

        assignDefaultFormatting(teamName);
        createScoreboardTeam(teamName);
        updatePlayerTabList(leader);

        plugin.getLogger().info("Team '" + teamName + "' created by " + leader.getName());
        return true;
    }

    public boolean inviteToTeam(Player inviter, Player target, String teamName) {
        if (!isTeamLeader(inviter.getUniqueId(), teamName)) {
            inviter.sendMessage(Component.text("You must be the team leader to invite players!", NamedTextColor.RED));
            return false;
        }

        if (playerTeams.containsKey(target.getUniqueId())) {
            inviter.sendMessage(Component.text(target.getName() + " is already in a team!", NamedTextColor.RED));
            return false;
        }

        teamInvites.computeIfAbsent(target.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(teamName);
        plugin.getLogger().info(inviter.getName() + " invited " + target.getName() + " to team '" + teamName + "'");
        return true;
    }

    public boolean acceptTeamInvite(Player player, String teamName) {
        Set<String> invites = teamInvites.get(player.getUniqueId());
        if (invites == null || !invites.contains(teamName)) {
            player.sendMessage(Component.text("You don't have an invite from that team!", NamedTextColor.RED));
            return false;
        }

        Set<UUID> members = teams.get(teamName);
        if (members == null) {
            invites.remove(teamName);
            player.sendMessage(Component.text("That team no longer exists!", NamedTextColor.RED));
            return false;
        }

        if (playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a team!", NamedTextColor.RED));
            return false;
        }

        members.add(player.getUniqueId());
        playerTeams.put(player.getUniqueId(), teamName);
        invites.remove(teamName);

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

        if (isTeamLeader(player.getUniqueId(), teamName)) {
            disbandTeam(teamName);
            return true;
        }

        Set<UUID> members = teams.get(teamName);
        if (members != null) {
            members.remove(player.getUniqueId());
        }
        playerTeams.remove(player.getUniqueId());

        updatePlayerTabList(player);
        plugin.getLogger().info(player.getName() + " left team '" + teamName + "'");
        return true;
    }

    public boolean disbandTeam(String teamName) {
        Set<UUID> members = teams.get(teamName);
        if (members == null) return false;

        plugin.getLogger().info("Disbanding team '" + teamName + "'");

        for (UUID memberUUID : new ArrayList<>(members)) {
            playerTeams.remove(memberUUID);
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                updatePlayerTabList(member);
            }
        }

        // Clean up ally relationships
        Set<String> allies = teamAllies.remove(teamName);
        if (allies != null) {
            for (String allyTeam : allies) {
                Set<String> otherAllies = teamAllies.get(allyTeam);
                if (otherAllies != null) {
                    otherAllies.remove(teamName);
                }
            }
        }

        pendingAllyRequests.remove(teamName);

        teams.remove(teamName);
        teamLeaders.remove(teamName);
        teamColors.remove(teamName);
        teamBold.remove(teamName);
        teamItalic.remove(teamName);

        removeScoreboardTeam(teamName);
        return true;
    }

    public boolean kickFromTeam(Player leader, Player target, String teamName) {
        if (!isTeamLeader(leader.getUniqueId(), teamName)) {
            leader.sendMessage(Component.text("You must be the team leader to kick players!", NamedTextColor.RED));
            return false;
        }

        if (leader.equals(target)) {
            leader.sendMessage(Component.text("You cannot kick yourself! Use /team leave instead.", NamedTextColor.RED));
            return false;
        }

        Set<UUID> members = teams.get(teamName);
        if (members == null) return false;

        members.remove(target.getUniqueId());
        playerTeams.remove(target.getUniqueId());

        updatePlayerTabList(target);
        plugin.getLogger().info(leader.getName() + " kicked " + target.getName() + " from team '" + teamName + "'");
        return true;
    }

    // ========================================
    // TEAM CUSTOMIZATION (same as before)
    // ========================================

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

    private void refreshTeamDisplay(String teamName) {
        removeScoreboardTeam(teamName);
        createScoreboardTeam(teamName);

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

    public TextColor getTeamColor(String teamName) {
        return teamColors.get(teamName);
    }

    public boolean isTeamBold(String teamName) {
        return teamBold.getOrDefault(teamName, false);
    }

    public boolean isTeamItalic(String teamName) {
        return teamItalic.getOrDefault(teamName, false);
    }

    /**
     * Get team member names for display (for backwards compatibility)
     */
    public List<String> getTeamMemberNames(UUID owner) {
        String teamName = playerTeams.get(owner);
        if (teamName == null) return new ArrayList<>();

        Set<UUID> members = teams.get(teamName);
        if (members == null) return new ArrayList<>();

        return members.stream()
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ========================================
    // TAB LIST MANAGEMENT (same as before)
    // ========================================

    public void updatePlayerTabList(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        Team existingTeam = scoreboard.getPlayerTeam(player);
        if (existingTeam != null) {
            existingTeam.removeEntry(player.getName());
        }

        if (teamName != null) {
            Team scoreboardTeam = getOrCreateScoreboardTeam(teamName);
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

        Team existing = scoreboard.getTeam(teamName);
        if (existing != null) {
            existing.unregister();
        }

        Team scoreboardTeam = scoreboard.registerNewTeam(teamName);

        TextColor color = teamColors.getOrDefault(teamName, NamedTextColor.WHITE);
        boolean bold = teamBold.getOrDefault(teamName, false);
        boolean italic = teamItalic.getOrDefault(teamName, false);

        Component prefix = Component.text("[" + teamName + "] ", color);

        if (bold) {
            prefix = prefix.decoration(TextDecoration.BOLD, true);
        }
        if (italic) {
            prefix = prefix.decoration(TextDecoration.ITALIC, true);
        }

        scoreboardTeam.prefix(prefix);

        if (color instanceof NamedTextColor namedColor) {
            scoreboardTeam.color(namedColor);
        } else {
            scoreboardTeam.color(NamedTextColor.WHITE);
        }

        scoreboardTeam.setAllowFriendlyFire(false);
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
        if (colorStr.startsWith("#")) {
            try {
                return TextColor.fromHexString(colorStr);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

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
        teamInvites.remove(playerUUID);
    }
}