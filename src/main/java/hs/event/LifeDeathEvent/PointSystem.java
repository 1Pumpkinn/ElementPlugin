package hs.event.LifeDeathEvent;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages player points for Life/Death event
 * Tracks passive and hostile mob kills
 */
public class PointSystem {
    private final Plugin plugin;
    private final File dataFile;
    private FileConfiguration data;

    // UUID -> passive mob kills
    private final Map<UUID, Integer> passiveKills = new HashMap<>();
    // UUID -> hostile mob kills
    private final Map<UUID, Integer> hostileKills = new HashMap<>();

    private boolean eventActive = false;

    public PointSystem(Plugin plugin) {
        this.plugin = plugin;

        // Create event data directory
        File eventDir = new File(plugin.getDataFolder(), "event");
        if (!eventDir.exists()) {
            eventDir.mkdirs();
        }

        this.dataFile = new File(eventDir, "scores.yml");
        loadData();
    }

    /**
     * Load scores from file
     */
    private void loadData() {
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create event scores file", e);
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);

        // Load passive kills
        if (data.contains("passive")) {
            for (String key : data.getConfigurationSection("passive").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int kills = data.getInt("passive." + key);
                    passiveKills.put(uuid, kills);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in passive kills: " + key);
                }
            }
        }

        // Load hostile kills
        if (data.contains("hostile")) {
            for (String key : data.getConfigurationSection("hostile").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int kills = data.getInt("hostile." + key);
                    hostileKills.put(uuid, kills);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in hostile kills: " + key);
                }
            }
        }

        // Load event active state
        eventActive = data.getBoolean("event_active", false);
    }

    /**
     * Save scores to file
     */
    public void saveData() {
        // Save passive kills
        data.set("passive", null); // Clear old data
        for (Map.Entry<UUID, Integer> entry : passiveKills.entrySet()) {
            data.set("passive." + entry.getKey().toString(), entry.getValue());
        }

        // Save hostile kills
        data.set("hostile", null); // Clear old data
        for (Map.Entry<UUID, Integer> entry : hostileKills.entrySet()) {
            data.set("hostile." + entry.getKey().toString(), entry.getValue());
        }

        // Save event active state
        data.set("event_active", eventActive);

        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save event scores", e);
        }
    }

    /**
     * Start the event
     */
    public void startEvent() {
        eventActive = true;
        passiveKills.clear();
        hostileKills.clear();
        saveData();
    }

    /**
     * End the event
     */
    public void endEvent() {
        eventActive = false;
        saveData();
    }

    /**
     * Check if event is active
     */
    public boolean isEventActive() {
        return eventActive;
    }

    /**
     * Add passive mob kill for player
     */
    public void addPassiveKill(UUID player) {
        if (!eventActive) return;
        passiveKills.merge(player, 1, Integer::sum);
        saveData();
    }

    /**
     * Add hostile mob kill for player
     */
    public void addHostileKill(UUID player) {
        if (!eventActive) return;
        hostileKills.merge(player, 1, Integer::sum);
        saveData();
    }

    /**
     * Get passive kills for player
     */
    public int getPassiveKills(UUID player) {
        return passiveKills.getOrDefault(player, 0);
    }

    /**
     * Get hostile kills for player
     */
    public int getHostileKills(UUID player) {
        return hostileKills.getOrDefault(player, 0);
    }

    /**
     * Get top player for passive kills
     * @return UUID of top player, or null if no kills
     */
    public UUID getTopPassivePlayer() {
        return passiveKills.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Get top player for hostile kills
     * @return UUID of top player, or null if no kills
     */
    public UUID getTopHostilePlayer() {
        return hostileKills.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Get sorted list of passive kill leaders
     * @return List of entries sorted by kills (descending)
     */
    public List<Map.Entry<UUID, Integer>> getPassiveLeaderboard() {
        return passiveKills.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();
    }

    /**
     * Get sorted list of hostile kill leaders
     * @return List of entries sorted by kills (descending)
     */
    public List<Map.Entry<UUID, Integer>> getHostileLeaderboard() {
        return hostileKills.entrySet().stream()
                .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
                .toList();
    }

    /**
     * Reset all scores (doesn't affect event active state)
     */
    public void resetScores() {
        passiveKills.clear();
        hostileKills.clear();
        saveData();
    }
}