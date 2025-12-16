package saturn.elementPlugin.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Represents all team-related data
 * Stores teams, membership, leaders, invites, allies, and customization
 */
public class TeamData {

    // Team membership: UUID -> Team Name
    private final Map<UUID, String> playerTeams = new HashMap<>();

    // Team members: Team Name -> Set of UUIDs
    private final Map<String, Set<UUID>> teams = new HashMap<>();

    // Team leaders: Team Name -> Leader UUID
    private final Map<String, UUID> teamLeaders = new HashMap<>();

    // Team invites: Player UUID -> Set of Team Names they're invited to
    private final Map<UUID, Set<String>> teamInvites = new HashMap<>();

    // Team allies: Team Name -> Allied Team Name (ONE ally per team)
    private final Map<String, String> teamAllies = new HashMap<>();

    // Team customization
    private final Map<String, String> teamColors = new HashMap<>();
    private final Map<String, Boolean> teamBold = new HashMap<>();
    private final Map<String, Boolean> teamItalic = new HashMap<>();

    // Individual trust (in-memory, not persisted in TeamData)
    // This is managed by IndividualTrustManager separately

    // Dirty flag for efficient saving
    private transient boolean dirty = false;

    /**
     * Create empty TeamData
     */
    public TeamData() {
    }

    /**
     * Load TeamData from configuration section
     */
    public TeamData(ConfigurationSection section) {
        if (section == null) return;

        // Load player teams
        ConfigurationSection playerTeamsSection = section.getConfigurationSection("playerTeams");
        if (playerTeamsSection != null) {
            for (String uuidStr : playerTeamsSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String teamName = playerTeamsSection.getString(uuidStr);
                    playerTeams.put(uuid, teamName);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }

        // Load teams and their members
        ConfigurationSection teamsSection = section.getConfigurationSection("teams");
        if (teamsSection != null) {
            for (String teamName : teamsSection.getKeys(false)) {
                List<String> memberStrings = teamsSection.getStringList(teamName);
                Set<UUID> members = new HashSet<>();
                for (String uuidStr : memberStrings) {
                    try {
                        members.add(UUID.fromString(uuidStr));
                    } catch (IllegalArgumentException e) {
                        // Skip invalid UUIDs
                    }
                }
                teams.put(teamName, members);
            }
        }

        // Load team leaders
        ConfigurationSection leadersSection = section.getConfigurationSection("teamLeaders");
        if (leadersSection != null) {
            for (String teamName : leadersSection.getKeys(false)) {
                try {
                    UUID leaderUUID = UUID.fromString(leadersSection.getString(teamName));
                    teamLeaders.put(teamName, leaderUUID);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }

        // Load team invites
        ConfigurationSection invitesSection = section.getConfigurationSection("teamInvites");
        if (invitesSection != null) {
            for (String uuidStr : invitesSection.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    List<String> invites = invitesSection.getStringList(uuidStr);
                    teamInvites.put(uuid, new HashSet<>(invites));
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }

        // Load team allies
        ConfigurationSection alliesSection = section.getConfigurationSection("teamAllies");
        if (alliesSection != null) {
            for (String teamName : alliesSection.getKeys(false)) {
                String allyTeam = alliesSection.getString(teamName);
                teamAllies.put(teamName, allyTeam);
            }
        }

        // Load team customization
        ConfigurationSection colorsSection = section.getConfigurationSection("teamColors");
        if (colorsSection != null) {
            for (String teamName : colorsSection.getKeys(false)) {
                teamColors.put(teamName, colorsSection.getString(teamName));
            }
        }

        ConfigurationSection boldSection = section.getConfigurationSection("teamBold");
        if (boldSection != null) {
            for (String teamName : boldSection.getKeys(false)) {
                teamBold.put(teamName, boldSection.getBoolean(teamName));
            }
        }

        ConfigurationSection italicSection = section.getConfigurationSection("teamItalic");
        if (italicSection != null) {
            for (String teamName : italicSection.getKeys(false)) {
                teamItalic.put(teamName, italicSection.getBoolean(teamName));
            }
        }

        this.dirty = false;
    }

    // ========================================
    // PLAYER TEAMS
    // ========================================

    public Map<UUID, String> getPlayerTeams() {
        return playerTeams;
    }

    public String getPlayerTeam(UUID uuid) {
        return playerTeams.get(uuid);
    }

    public void setPlayerTeam(UUID uuid, String teamName) {
        if (teamName == null) {
            playerTeams.remove(uuid);
        } else {
            playerTeams.put(uuid, teamName);
        }
        markDirty();
    }

    // ========================================
    // TEAMS & MEMBERS
    // ========================================

    public Map<String, Set<UUID>> getTeams() {
        return teams;
    }

    public Set<UUID> getTeamMembers(String teamName) {
        return teams.getOrDefault(teamName, new HashSet<>());
    }

    public void createTeam(String teamName, Set<UUID> members) {
        teams.put(teamName, new HashSet<>(members));
        markDirty();
    }

    public void removeTeam(String teamName) {
        teams.remove(teamName);
        markDirty();
    }

    public void addTeamMember(String teamName, UUID member) {
        teams.computeIfAbsent(teamName, k -> new HashSet<>()).add(member);
        markDirty();
    }

    public void removeTeamMember(String teamName, UUID member) {
        Set<UUID> members = teams.get(teamName);
        if (members != null) {
            members.remove(member);
            markDirty();
        }
    }

    // ========================================
    // TEAM LEADERS
    // ========================================

    public Map<String, UUID> getTeamLeaders() {
        return teamLeaders;
    }

    public UUID getTeamLeader(String teamName) {
        return teamLeaders.get(teamName);
    }

    public void setTeamLeader(String teamName, UUID leader) {
        teamLeaders.put(teamName, leader);
        markDirty();
    }

    public void removeTeamLeader(String teamName) {
        teamLeaders.remove(teamName);
        markDirty();
    }

    // ========================================
    // TEAM INVITES
    // ========================================

    public Map<UUID, Set<String>> getTeamInvites() {
        return teamInvites;
    }

    public Set<String> getPlayerInvites(UUID uuid) {
        return teamInvites.getOrDefault(uuid, new HashSet<>());
    }

    public void addInvite(UUID player, String teamName) {
        teamInvites.computeIfAbsent(player, k -> new HashSet<>()).add(teamName);
        markDirty();
    }

    public void removeInvite(UUID player, String teamName) {
        Set<String> invites = teamInvites.get(player);
        if (invites != null) {
            invites.remove(teamName);
            markDirty();
        }
    }

    public void clearPlayerInvites(UUID player) {
        teamInvites.remove(player);
        markDirty();
    }

    // ========================================
    // TEAM ALLIES
    // ========================================

    public Map<String, String> getTeamAllies() {
        return teamAllies;
    }

    public String getAllyTeam(String teamName) {
        return teamAllies.get(teamName);
    }

    public void setAlly(String teamName, String allyTeam) {
        teamAllies.put(teamName, allyTeam);
        markDirty();
    }

    public void removeAlly(String teamName) {
        teamAllies.remove(teamName);
        markDirty();
    }

    // ========================================
    // TEAM CUSTOMIZATION
    // ========================================

    public String getTeamColor(String teamName) {
        return teamColors.get(teamName);
    }

    public void setTeamColor(String teamName, String color) {
        teamColors.put(teamName, color);
        markDirty();
    }

    public Boolean isTeamBold(String teamName) {
        return teamBold.get(teamName);
    }

    public void setTeamBold(String teamName, boolean bold) {
        teamBold.put(teamName, bold);
        markDirty();
    }

    public Boolean isTeamItalic(String teamName) {
        return teamItalic.get(teamName);
    }

    public void setTeamItalic(String teamName, boolean italic) {
        teamItalic.put(teamName, italic);
        markDirty();
    }

    public void removeTeamCustomization(String teamName) {
        teamColors.remove(teamName);
        teamBold.remove(teamName);
        teamItalic.remove(teamName);
        markDirty();
    }

    // ========================================
    // DIRTY FLAG SYSTEM
    // ========================================

    public boolean isDirty() {
        return dirty;
    }

    private void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    // ========================================
    // SERIALIZATION
    // ========================================

    public void saveTo(ConfigurationSection section) {
        // Save player teams
        section.set("playerTeams", null); // Clear existing
        ConfigurationSection playerTeamsSection = section.createSection("playerTeams");
        for (Map.Entry<UUID, String> entry : playerTeams.entrySet()) {
            playerTeamsSection.set(entry.getKey().toString(), entry.getValue());
        }

        // Save teams and their members
        section.set("teams", null);
        ConfigurationSection teamsSection = section.createSection("teams");
        for (Map.Entry<String, Set<UUID>> entry : teams.entrySet()) {
            List<String> memberStrings = new ArrayList<>();
            for (UUID uuid : entry.getValue()) {
                memberStrings.add(uuid.toString());
            }
            teamsSection.set(entry.getKey(), memberStrings);
        }

        // Save team leaders
        section.set("teamLeaders", null);
        ConfigurationSection leadersSection = section.createSection("teamLeaders");
        for (Map.Entry<String, UUID> entry : teamLeaders.entrySet()) {
            leadersSection.set(entry.getKey(), entry.getValue().toString());
        }

        // Save team invites
        section.set("teamInvites", null);
        ConfigurationSection invitesSection = section.createSection("teamInvites");
        for (Map.Entry<UUID, Set<String>> entry : teamInvites.entrySet()) {
            invitesSection.set(entry.getKey().toString(), new ArrayList<>(entry.getValue()));
        }

        // Save team allies
        section.set("teamAllies", null);
        ConfigurationSection alliesSection = section.createSection("teamAllies");
        for (Map.Entry<String, String> entry : teamAllies.entrySet()) {
            alliesSection.set(entry.getKey(), entry.getValue());
        }

        // Save team customization
        section.set("teamColors", null);
        ConfigurationSection colorsSection = section.createSection("teamColors");
        for (Map.Entry<String, String> entry : teamColors.entrySet()) {
            colorsSection.set(entry.getKey(), entry.getValue());
        }

        section.set("teamBold", null);
        ConfigurationSection boldSection = section.createSection("teamBold");
        for (Map.Entry<String, Boolean> entry : teamBold.entrySet()) {
            boldSection.set(entry.getKey(), entry.getValue());
        }

        section.set("teamItalic", null);
        ConfigurationSection italicSection = section.createSection("italicItalic");
        for (Map.Entry<String, Boolean> entry : teamItalic.entrySet()) {
            italicSection.set(entry.getKey(), entry.getValue());
        }

        markClean();
    }
}