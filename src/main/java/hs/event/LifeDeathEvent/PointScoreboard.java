package hs.event.LifeDeathEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointScoreboard {
    private final Plugin plugin;
    private final PointSystem pointSystem;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private int taskId = -1;

    public PointScoreboard(Plugin plugin, PointSystem pointSystem) {
        this.plugin = plugin;
        this.pointSystem = pointSystem;
    }

    public void showScoreboard(Player player) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "lifedeath",
                Criteria.DUMMY,
                ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ LIFE vs DEATH ⚔"
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);

        updateScoreboard(player);

        if (taskId == -1) {
            startUpdateTask();
        }
    }

    public void hideScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());

        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }

        if (playerScoreboards.isEmpty() && taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("lifedeath");
        if (objective == null) return;

        // Clear old entries
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        int passiveKills = pointSystem.getPassiveKills(player.getUniqueId());
        int hostileKills = pointSystem.getHostileKills(player.getUniqueId());

        UUID topPassive = pointSystem.getTopPassivePlayer();
        UUID topHostile = pointSystem.getTopHostilePlayer();

        int topPassiveKills = topPassive != null ? pointSystem.getPassiveKills(topPassive) : 0;
        int topHostileKills = topHostile != null ? pointSystem.getHostileKills(topHostile) : 0;

        String topPassiveName = "None";
        String topHostileName = "None";

        if (topPassive != null) {
            Player topPassivePlayer = Bukkit.getPlayer(topPassive);
            topPassiveName = topPassivePlayer != null ? topPassivePlayer.getName() : "Offline";
            if (topPassiveName.length() > 12) {
                topPassiveName = topPassiveName.substring(0, 12);
            }
        }

        if (topHostile != null) {
            Player topHostilePlayer = Bukkit.getPlayer(topHostile);
            topHostileName = topHostilePlayer != null ? topHostilePlayer.getName() : "Offline";
            if (topHostileName.length() > 12) {
                topHostileName = topHostileName.substring(0, 12);
            }
        }

        // Define lines top to bottom
        String[] lines = new String[]{
                ChatColor.GOLD + "━━━━━━━━━━━━━━━",
                "",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "YOUR STATS",
                ChatColor.GREEN + "🌿 " + ChatColor.WHITE + passiveKills + " passive",
                ChatColor.DARK_PURPLE + "💀 " + ChatColor.WHITE + hostileKills + " hostile",
                "",
                ChatColor.YELLOW + "" + ChatColor.BOLD + "TOP PLAYERS",
                ChatColor.GREEN + "🌿 " + ChatColor.WHITE + topPassiveName,
                ChatColor.GRAY + "   " + topPassiveKills + " kills",
                ChatColor.DARK_PURPLE + "💀 " + ChatColor.WHITE + topHostileName,
                ChatColor.GRAY + "   " + topHostileKills + " kills",
                "",
                ChatColor.GOLD + "━━━━━━━━━━━━━━━"
        };

        int lineScore = lines.length;
        for (String line : lines) {
            // Create unique blank entry using different amounts of invisible characters
            String entry = createBlankEntry(lineScore);

            // Register team for this line
            Team team = scoreboard.getTeam("line" + lineScore);
            if (team == null) {
                team = scoreboard.registerNewTeam("line" + lineScore);
            }

            if (!team.hasEntry(entry)) {
                team.addEntry(entry);
            }

            // Set the line text using team prefix/suffix to avoid 16 character limit
            if (line.length() <= 16) {
                team.setPrefix(line);
                team.setSuffix("");
            } else {
                team.setPrefix(line.substring(0, 16));
                team.setSuffix(line.substring(16));
            }

            // Set score to maintain order (higher = higher on board)
            objective.getScore(entry).setScore(lineScore);
            lineScore--;
        }
    }

    /**
     * Creates a unique blank entry using invisible characters
     * This prevents the red numbers from showing on the scoreboard
     */
    private String createBlankEntry(int index) {
        // Use color codes as invisible unique identifiers
        // Each line gets a unique combination of color codes that render as blank
        StringBuilder blank = new StringBuilder();

        // Create unique blank string by repeating color reset codes
        for (int i = 0; i < index; i++) {
            blank.append(ChatColor.RESET);
        }

        return blank.toString();
    }

    private void startUpdateTask() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (UUID uuid : playerScoreboards.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    updateScoreboard(player);
                }
            }
        }, 0L, 40L); // update every 2 seconds
    }

    public void stopUpdateTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void cleanup() {
        for (UUID uuid : new HashMap<>(playerScoreboards).keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                hideScoreboard(player);
            }
        }
        stopUpdateTask();
    }
}