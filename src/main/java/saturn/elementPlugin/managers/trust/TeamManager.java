package saturn.elementPlugin.managers.trust;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.TeamData;
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

public class TeamManager {

    private final ElementPlugin plugin;
    private final TeamData teamData;

    // UPDATED: Added team size limit constant
    private static final int MAX_TEAM_SIZE = 5;

    private final Map<UUID, String> playerTeams;
    private final Map<String, Set<UUID>> teams;
    private final Map<String, UUID> teamLeaders;
    private final Map<UUID, Set<String>> teamInvites;
    private final Map<String, String> teamAllies;

    private final Map<String, TextColor> teamColors = new ConcurrentHashMap<>();
    private final Map<String, Boolean> teamBold = new ConcurrentHashMap<>();
    private final Map<String, Boolean> teamItalic = new ConcurrentHashMap<>();

    // ✅ TEAM HIDE STATE (PER PLAYER)
    private final Map<UUID, Boolean> teamHidden = new ConcurrentHashMap<>();

    private static final NamedTextColor[] DEFAULT_COLORS = {
            NamedTextColor.AQUA, NamedTextColor.GREEN, NamedTextColor.YELLOW,
            NamedTextColor.LIGHT_PURPLE, NamedTextColor.GOLD, NamedTextColor.RED,
            NamedTextColor.DARK_AQUA, NamedTextColor.DARK_GREEN, NamedTextColor.BLUE
    };

    private int colorIndex = 0;

    public TeamManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.teamData = plugin.getDataStore().getTeamData();

        this.playerTeams = teamData.getPlayerTeams();
        this.teams = teamData.getTeams();
        this.teamLeaders = teamData.getTeamLeaders();
        this.teamInvites = teamData.getTeamInvites();
        this.teamAllies = teamData.getTeamAllies();

        // Load customization data
        loadCustomizationData();

        // FIXED: Delay scoreboard recreation until server is fully loaded
        // Schedule for next tick to ensure ScoreboardManager is available
        Bukkit.getScheduler().runTask(plugin, this::recreateScoreboardTeams);
    }

    // ========================================
    // TEAM VISIBILITY (HIDE PREFIX)
    // ========================================

    public boolean isTeamHidden(UUID playerUUID) {
        return teamHidden.getOrDefault(playerUUID, false);
    }

    public void toggleTeamHidden(UUID playerUUID) {
        boolean current = teamHidden.getOrDefault(playerUUID, false);
        setTeamHidden(playerUUID, !current);
    }

    // Set hidden state and save
    public void setTeamHidden(UUID playerUUID, boolean hidden) {
        teamHidden.put(playerUUID, hidden);
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            updatePlayerTabList(player);
        }
        saveTeamHiddenData();
    }

    private void saveTeamHiddenData() {
        plugin.getDataStore().saveTeamHidden(teamHidden);
    }

    public void loadTeamHiddenData() {
        Map<UUID, Boolean> loaded = plugin.getDataStore().loadTeamHidden();
        if (loaded != null) {
            teamHidden.clear();
            teamHidden.putAll(loaded);
        }
    }

    /**
     * Load team customization data from TeamData
     */
    private void loadCustomizationData() {
        for (String teamName : teams.keySet()) {
            String colorStr = teamData.getTeamColor(teamName);
            if (colorStr != null) {
                TextColor color = parseColor(colorStr);
                if (color != null) {
                    teamColors.put(teamName, color);
                }
            }

            Boolean bold = teamData.isTeamBold(teamName);
            if (bold != null) {
                teamBold.put(teamName, bold);
            }

            Boolean italic = teamData.isTeamItalic(teamName);
            if (italic != null) {
                teamItalic.put(teamName, italic);
            }
        }

        plugin.getLogger().info("Loaded customization for " + teamColors.size() + " teams");
    }

    /**
     * Recreate all scoreboard teams on server startup
     */
    private void recreateScoreboardTeams() {
        for (String teamName : teams.keySet()) {
            createScoreboardTeam(teamName);
        }

        // Update tab list for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTabList(player);
        }

        plugin.getLogger().info("Recreated " + teams.size() + " scoreboard teams");
    }

    /**
     * Save team data to disk
     * Call this when team data changes or on server shutdown
     */
    private void saveTeamData() {
        // Save customization data back to TeamData
        for (Map.Entry<String, TextColor> entry : teamColors.entrySet()) {
            String colorStr = entry.getValue() instanceof NamedTextColor
                    ? ((NamedTextColor) entry.getValue()).toString()
                    : entry.getValue().asHexString();
            teamData.setTeamColor(entry.getKey(), colorStr);
        }

        // Save to disk
        plugin.getDataStore().saveTeamData();
    }

    // ========================================
    // TEAM CHECKS
    // ========================================

    public boolean areOnSameTeam(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);
        return team1 != null && team1.equals(team2);
    }

    public boolean areAllies(UUID player1, UUID player2) {
        String team1 = playerTeams.get(player1);
        String team2 = playerTeams.get(player2);

        if (team1 == null || team2 == null || team1.equals(team2)) {
            return false;
        }

        String team1Ally = teamAllies.get(team1);
        if (team1Ally != null && team1Ally.equals(team2)) {
            return true;
        }

        String team2Ally = teamAllies.get(team2);
        return team2Ally != null && team2Ally.equals(team1);
    }

    // ========================================
    // ALLY SYSTEM
    // ========================================

    public boolean addAlly(Player requester, String targetTeamName) {
        String requesterTeam = playerTeams.get(requester.getUniqueId());

        if (requesterTeam == null) {
            requester.sendMessage(Component.text("You must be in a team to ally with others!", NamedTextColor.RED));
            return false;
        }

        if (!isTeamLeader(requester.getUniqueId(), requesterTeam)) {
            requester.sendMessage(Component.text("Only the team leader can manage allies!", NamedTextColor.RED));
            return false;
        }

        if (!teams.containsKey(targetTeamName)) {
            requester.sendMessage(Component.text("Team '" + targetTeamName + "' does not exist!", NamedTextColor.RED));
            return false;
        }

        if (requesterTeam.equals(targetTeamName)) {
            requester.sendMessage(Component.text("You cannot ally with your own team!", NamedTextColor.RED));
            return false;
        }

        if (teamAllies.containsKey(requesterTeam)) {
            String currentAlly = teamAllies.get(requesterTeam);
            requester.sendMessage(Component.text("You can only have ONE ally! Current ally: ", NamedTextColor.RED)
                    .append(Component.text(currentAlly, NamedTextColor.AQUA)));
            return false;
        }

        if (teamAllies.containsKey(targetTeamName)) {
            requester.sendMessage(Component.text("That team already has an ally!", NamedTextColor.RED));
            return false;
        }

        teamData.setAlly(requesterTeam, targetTeamName);
        teamData.setAlly(targetTeamName, requesterTeam);
        saveTeamData();

        requester.sendMessage(Component.text("✓ Your team is now allied with ", NamedTextColor.GREEN)
                .append(Component.text(targetTeamName, NamedTextColor.AQUA, TextDecoration.BOLD)));

        UUID targetLeaderUUID = teamLeaders.get(targetTeamName);
        Player targetLeader = Bukkit.getPlayer(targetLeaderUUID);
        if (targetLeader != null) {
            targetLeader.sendMessage(Component.text("✓ Team ", NamedTextColor.GREEN)
                    .append(Component.text(requesterTeam, NamedTextColor.AQUA, TextDecoration.BOLD))
                    .append(Component.text(" is now your ally!", NamedTextColor.GREEN)));
        }

        plugin.getLogger().info("Teams '" + requesterTeam + "' and '" + targetTeamName + "' are now allies");
        return true;
    }

    public boolean removeAlly(Player player, String allyTeamName) {
        String playerTeam = playerTeams.get(player.getUniqueId());

        if (playerTeam == null) {
            player.sendMessage(Component.text("You must be in a team!", NamedTextColor.RED));
            return false;
        }

        if (!isTeamLeader(player.getUniqueId(), playerTeam)) {
            player.sendMessage(Component.text("Only the team leader can remove allies!", NamedTextColor.RED));
            return false;
        }

        String currentAlly = teamAllies.get(playerTeam);
        if (currentAlly == null || !currentAlly.equals(allyTeamName)) {
            player.sendMessage(Component.text("Your team is not allied with " + allyTeamName, NamedTextColor.RED));
            return false;
        }

        teamData.removeAlly(playerTeam);
        teamData.removeAlly(allyTeamName);
        saveTeamData();

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

    public String getAllyTeam(UUID playerUUID) {
        String team = playerTeams.get(playerUUID);
        if (team == null) return null;
        return teamAllies.get(team);
    }

    // ========================================
    // TEAM CREATION & MANAGEMENT
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

        teamData.createTeam(teamName, members);
        teamData.setTeamLeader(teamName, leader.getUniqueId());
        teamData.setPlayerTeam(leader.getUniqueId(), teamName);

        assignDefaultFormatting(teamName);
        createScoreboardTeam(teamName);
        updatePlayerTabList(leader);

        saveTeamData();

        plugin.getLogger().info("Team '" + teamName + "' created by " + leader.getName());
        return true;
    }

    // UPDATED: Check team size before inviting
    public boolean inviteToTeam(Player inviter, Player target, String teamName) {
        if (!isTeamLeader(inviter.getUniqueId(), teamName)) {
            inviter.sendMessage(Component.text("You must be the team leader to invite players!", NamedTextColor.RED));
            return false;
        }

        if (playerTeams.containsKey(target.getUniqueId())) {
            inviter.sendMessage(Component.text(target.getName() + " is already in a team!", NamedTextColor.RED));
            return false;
        }

        // UPDATED: Check team size limit
        Set<UUID> currentMembers = teams.get(teamName);
        if (currentMembers != null && currentMembers.size() >= MAX_TEAM_SIZE) {
            inviter.sendMessage(Component.text("Your team is full! Maximum team size is " + MAX_TEAM_SIZE + " players.", NamedTextColor.RED));
            return false;
        }

        teamData.addInvite(target.getUniqueId(), teamName);
        saveTeamData();

        plugin.getLogger().info(inviter.getName() + " invited " + target.getName() + " to team '" + teamName + "'");
        return true;
    }

    // UPDATED: Check team size before accepting
    public boolean acceptTeamInvite(Player player, String teamName) {
        Set<String> invites = teamInvites.get(player.getUniqueId());
        if (invites == null || !invites.contains(teamName)) {
            player.sendMessage(Component.text("You don't have an invite from that team!", NamedTextColor.RED));
            return false;
        }

        Set<UUID> members = teams.get(teamName);
        if (members == null) {
            teamData.removeInvite(player.getUniqueId(), teamName);
            player.sendMessage(Component.text("That team no longer exists!", NamedTextColor.RED));
            return false;
        }

        // UPDATED: Check team size limit before accepting
        if (members.size() >= MAX_TEAM_SIZE) {
            player.sendMessage(Component.text("That team is full! Maximum team size is " + MAX_TEAM_SIZE + " players.", NamedTextColor.RED));
            teamData.removeInvite(player.getUniqueId(), teamName);
            return false;
        }

        if (playerTeams.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("You are already in a team!", NamedTextColor.RED));
            return false;
        }

        teamData.addTeamMember(teamName, player.getUniqueId());
        teamData.setPlayerTeam(player.getUniqueId(), teamName);
        teamData.removeInvite(player.getUniqueId(), teamName);

        updatePlayerTabList(player);
        saveTeamData();

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

        teamData.removeTeamMember(teamName, player.getUniqueId());
        teamData.setPlayerTeam(player.getUniqueId(), null);

        updatePlayerTabList(player);
        saveTeamData();

        plugin.getLogger().info(player.getName() + " left team '" + teamName + "'");
        return true;
    }

    public boolean disbandTeam(String teamName) {
        Set<UUID> members = teams.get(teamName);
        if (members == null) return false;

        plugin.getLogger().info("Disbanding team '" + teamName + "'");

        for (UUID memberUUID : new ArrayList<>(members)) {
            teamData.setPlayerTeam(memberUUID, null);
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                updatePlayerTabList(member);
            }
        }

        // Clean up ally relationship
        String allyTeam = teamAllies.remove(teamName);
        if (allyTeam != null) {
            teamData.removeAlly(allyTeam);
        }

        teamData.removeTeam(teamName);
        teamData.removeTeamLeader(teamName);
        teamData.removeTeamCustomization(teamName);

        teamColors.remove(teamName);
        teamBold.remove(teamName);
        teamItalic.remove(teamName);

        removeScoreboardTeam(teamName);
        saveTeamData();

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

        teamData.removeTeamMember(teamName, target.getUniqueId());
        teamData.setPlayerTeam(target.getUniqueId(), null);

        updatePlayerTabList(target);
        saveTeamData();

        plugin.getLogger().info(leader.getName() + " kicked " + target.getName() + " from team '" + teamName + "'");
        return true;
    }

    // ========================================
    // TEAM CUSTOMIZATION
    // ========================================

    public boolean renameTeam(Player leader, String oldName, String newName) {
        if (!isTeamLeader(leader.getUniqueId(), oldName)) {
            leader.sendMessage(Component.text("Only the team leader can rename the team!", NamedTextColor.RED));
            return false;
        }

        if (!isValidTeamName(newName)) {
            leader.sendMessage(Component.text("Invalid team name! Must be 1-16 characters, no spaces.", NamedTextColor.RED));
            return false;
        }

        if (teams.containsKey(newName)) {
            leader.sendMessage(Component.text("A team with that name already exists!", NamedTextColor.RED));
            return false;
        }

        Set<UUID> members = teams.remove(oldName);
        UUID leaderUUID = teamLeaders.remove(oldName);
        TextColor color = teamColors.remove(oldName);
        Boolean bold = teamBold.remove(oldName);
        Boolean italic = teamItalic.remove(oldName);
        String ally = teamAllies.remove(oldName);

        teamData.removeTeam(oldName);
        teamData.removeTeamLeader(oldName);
        teamData.removeTeamCustomization(oldName);

        teamData.createTeam(newName, members);
        teamData.setTeamLeader(newName, leaderUUID);

        if (color != null) teamColors.put(newName, color);
        if (bold != null) {
            teamBold.put(newName, bold);
            teamData.setTeamBold(newName, bold);
        }
        if (italic != null) {
            teamItalic.put(newName, italic);
            teamData.setTeamItalic(newName, italic);
        }
        if (ally != null) {
            teamData.setAlly(newName, ally);
            teamData.setAlly(ally, newName);
        }

        for (UUID memberUUID : members) {
            teamData.setPlayerTeam(memberUUID, newName);
        }

        removeScoreboardTeam(oldName);
        createScoreboardTeam(newName);

        for (UUID memberUUID : members) {
            Player member = Bukkit.getPlayer(memberUUID);
            if (member != null) {
                updatePlayerTabList(member);
            }
        }

        saveTeamData();
        plugin.getLogger().info("Team '" + oldName + "' renamed to '" + newName + "'");
        return true;
    }

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
        teamData.setTeamColor(teamName, color);
        refreshTeamDisplay(teamName);
        saveTeamData();

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
        teamData.setTeamBold(teamName, !currentBold);
        refreshTeamDisplay(teamName);
        saveTeamData();

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
        teamData.setTeamItalic(teamName, !currentItalic);
        refreshTeamDisplay(teamName);
        saveTeamData();

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
    // TAB LIST MANAGEMENT
    // ========================================

    public void updatePlayerTabList(Player player) {
        String teamName = playerTeams.get(player.getUniqueId());

        org.bukkit.scoreboard.ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            plugin.getLogger().warning("ScoreboardManager not available - cannot update tab for " + player.getName());
            return;
        }

        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        // Always remove player first
        Team existingTeam = scoreboard.getPlayerTeam(player);
        if (existingTeam != null) {
            existingTeam.removeEntry(player.getName());
        }

        // Only add back if NOT hidden
        if (teamName != null && !isTeamHidden(player.getUniqueId())) {
            Team scoreboardTeam = getOrCreateScoreboardTeam(teamName);
            if (scoreboardTeam != null) {
                scoreboardTeam.addEntry(player.getName());
            }
        }
    }

    private Team getOrCreateScoreboardTeam(String teamName) {
        org.bukkit.scoreboard.ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            plugin.getLogger().warning("ScoreboardManager not available - cannot get team '" + teamName + "'");
            return null;
        }

        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();
        Team scoreboardTeam = scoreboard.getTeam(teamName);

        if (scoreboardTeam == null) {
            scoreboardTeam = createScoreboardTeam(teamName);
        }

        return scoreboardTeam;
    }

    private Team createScoreboardTeam(String teamName) {
        // FIXED: Add null check for ScoreboardManager
        org.bukkit.scoreboard.ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            plugin.getLogger().warning("ScoreboardManager not available yet - cannot create team '" + teamName + "'");
            return null;
        }

        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

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

        // FIXED: Set player name color to WHITE so only the prefix is colored
        scoreboardTeam.color(NamedTextColor.WHITE);

        scoreboardTeam.setAllowFriendlyFire(false);
        scoreboardTeam.setCanSeeFriendlyInvisibles(true);

        plugin.getLogger().info("Created scoreboard team '" + teamName + "' with custom formatting");

        return scoreboardTeam;
    }

    private void removeScoreboardTeam(String teamName) {
        org.bukkit.scoreboard.ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        if (scoreboardManager == null) {
            plugin.getLogger().warning("ScoreboardManager not available - cannot remove team '" + teamName + "'");
            return;
        }

        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();
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
        teamData.setTeamColor(teamName, color.toString());
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
                && !teamName.isEmpty()
                && teamName.length() <= 16
                && !teamName.contains(" ");
    }

    public void handlePlayerQuit(UUID playerUUID) {
        teamData.clearPlayerInvites(playerUUID);
        saveTeamData();
    }
}