package hs.event.LifeDeathEvent;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages scoreboard display for the Life/Death event
 */
public class PointScoreboard {
    private final Plugin plugin;
    private final PointSystem pointSystem;
    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private int taskId = -1;

    public PointScoreboard(Plugin plugin, PointSystem pointSystem) {
        this.plugin = plugin;
        this.pointSystem = pointSystem;
    }

    /**
     * Show scoreboard to a player
     */
    public void showScoreboard(Player player) {
        // Create new scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;

        Scoreboard scoreboard = manager.getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "lifedeath",
                Criteria.DUMMY,
                ChatColor.GOLD + "" + ChatColor.BOLD + "LIFE vs DEATH"
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Store scoreboard
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);

        // Update immediately
        updateScoreboard(player);

        // Start update task if not running
        if (taskId == -1) {
            startUpdateTask();
        }
    }

    /**
     * Hide scoreboard from a player
     */
    public void hideScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());

        // Reset to default scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager != null) {
            player.setScoreboard(manager.getMainScoreboard());
        }

        // Stop update task if no players have scoreboards
        if (playerScoreboards.isEmpty() && taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * Update scoreboard for a player
     */
    private void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("lifedeath");
        if (objective == null) return;

        // Clear existing scores
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get player's stats
        int passiveKills = pointSystem.getPassiveKills(player.getUniqueId());
        int hostileKills = pointSystem.getHostileKills(player.getUniqueId());

        // Get top players
        UUID topPassive = pointSystem.getTopPassivePlayer();
        UUID topHostile = pointSystem.getTopHostilePlayer();

        int topPassiveKills = topPassive != null ? pointSystem.getPassiveKills(topPassive) : 0;
        int topHostileKills = topHostile != null ? pointSystem.getHostileKills(topHostile) : 0;

        String topPassiveName = "None";
        String topHostileName = "None";

        if (topPassive != null) {
            Player topPassivePlayer = Bukkit.getPlayer(topPassive);
            topPassiveName = topPassivePlayer != null ? topPassivePlayer.getName() : "Offline";
            if (topPassiveName.length() > 10) {
                topPassiveName = topPassiveName.substring(0, 10);
            }
        }

        if (topHostile != null) {
            Player topHostilePlayer = Bukkit.getPlayer(topHostile);
            topHostileName = topHostilePlayer != null ? topHostilePlayer.getName() : "Offline";
            if (topHostileName.length() > 10) {
                topHostileName = topHostileName.substring(0, 10);
            }
        }

        // Build scoreboard (bottom to top)
        int line = 15;

        objective.getScore(ChatColor.GOLD + "═══════════════").setScore(line--);
        objective.getScore(" ").setScore(line--);

        // Your stats
        objective.getScore(ChatColor.YELLOW + "Your Stats:").setScore(line--);
        objective.getScore(ChatColor.GREEN + "🌿 Passive: " + ChatColor.WHITE + passiveKills).setScore(line--);
        objective.getScore(ChatColor.DARK_PURPLE + "💀 Hostile: " + ChatColor.WHITE + hostileKills).setScore(line--);
        objective.getScore("  ").setScore(line--);

        // Leaders
        objective.getScore(ChatColor.YELLOW + "Leaders:").setScore(line--);
        objective.getScore(ChatColor.GREEN + "🌿 " + topPassiveName).setScore(line--);
        objective.getScore(ChatColor.WHITE + "   " + topPassiveKills + " kills").setScore(line--);
        objective.getScore(ChatColor.DARK_PURPLE + "💀 " + topHostileName).setScore(line--);
        objective.getScore(ChatColor.WHITE + "   " + topHostileKills + " kills").setScore(line--);
        objective.getScore("   ").setScore(line--);

        objective.getScore(ChatColor.GOLD + "═══════════════").setScore(line--);
    }

    /**
     * Start the update task
     */
    private void startUpdateTask() {
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (UUID uuid : playerScoreboards.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null && player.isOnline()) {
                    updateScoreboard(player);
                }
            }
        }, 0L, 20L); // Update every second
    }

    /**
     * Stop the update task
     */
    public void stopUpdateTask() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    /**
     * Hide all scoreboards and stop updates
     */
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