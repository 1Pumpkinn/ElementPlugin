package hs.elementPlugin.data;

import hs.elementPlugin.ElementPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Handles all data persistence for the plugin.
 * Features:
 * - Automatic backup system
 * - Dirty flag optimization (only save changed data)
 * - Thread-safe operations
 * - Better error handling and recovery
 */
public class DataStore {
    private static final String PLAYERS_PATH = "players";
    private static final int AUTO_SAVE_INTERVAL_TICKS = 6000; // 5 minutes
    private static final int MAX_BACKUPS = 3;

    private final ElementPlugin plugin;
    private final File dataDir;
    private final File playerFile;
    private final File backupDir;

    private FileConfiguration playerConfig;

    // Thread-safe cache
    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();

    // Track last save time for debugging
    private long lastSaveTime = 0;

    public DataStore(ElementPlugin plugin) {
        this.plugin = plugin;

        // Setup directories
        this.dataDir = new File(plugin.getDataFolder(), "data");
        this.backupDir = new File(dataDir, "backups");

        ensureDirectoryExists(dataDir);
        ensureDirectoryExists(backupDir);

        // Setup files
        this.playerFile = new File(dataDir, "players.yml");
        ensureFileExists(playerFile);

        // Load configuration
        loadConfiguration();

        // Start auto-save task
        startAutoSave();

        plugin.getLogger().info("DataStore initialized successfully");
    }

    // ========================================
    // INITIALIZATION
    // ========================================

    private void ensureDirectoryExists(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Failed to create directory: " + dir.getAbsolutePath());
            }
        }
    }

    private void ensureFileExists(File file) {
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    throw new RuntimeException("Failed to create file: " + file.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new RuntimeException("Error creating file: " + file.getAbsolutePath(), e);
            }
        }
    }

    private void loadConfiguration() {
        try {
            this.playerConfig = YamlConfiguration.loadConfiguration(playerFile);
            plugin.getLogger().info("Loaded player configuration from disk");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player configuration: " + e.getMessage());

            // Try to recover from backup
            if (recoverFromBackup()) {
                plugin.getLogger().info("Successfully recovered from backup");
            } else {
                // Create new empty config
                this.playerConfig = new YamlConfiguration();
                plugin.getLogger().warning("Created new empty player configuration");
            }
        }
    }

    // ========================================
    // AUTO-SAVE SYSTEM
    // ========================================

    private void startAutoSave() {
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            try {
                saveAllDirty();
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during auto-save", e);
            }
        }, AUTO_SAVE_INTERVAL_TICKS, AUTO_SAVE_INTERVAL_TICKS);

        plugin.getLogger().info("Auto-save started (every 5 minutes)");
    }

    /**
     * Save only players with unsaved changes
     */
    private void saveAllDirty() {
        int savedCount = 0;

        for (PlayerData pd : playerDataCache.values()) {
            if (pd.isDirty()) {
                save(pd);
                savedCount++;
            }
        }

        if (savedCount > 0) {
            plugin.getLogger().info("Auto-saved " + savedCount + " player(s) with changes");
        }
    }

    // ========================================
    // PLAYER DATA OPERATIONS
    // ========================================

    /**
     * Get PlayerData for a UUID (cached)
     */
    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.computeIfAbsent(uuid, this::loadPlayerDataFromDisk);
    }

    /**
     * Load player data from disk
     */
    private PlayerData loadPlayerDataFromDisk(UUID uuid) {
        try {
            // Reload config to get latest data
            playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            String path = PLAYERS_PATH + "." + uuid.toString();
            ConfigurationSection section = playerConfig.getConfigurationSection(path);

            if (section == null) {
                // New player - create default data
                plugin.getLogger().fine("Creating new PlayerData for " + uuid);
                return new PlayerData(uuid);
            }

            // Load existing data
            plugin.getLogger().fine("Loaded PlayerData for " + uuid + " from disk");
            return new PlayerData(uuid, section);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load PlayerData for " + uuid, e);
            return new PlayerData(uuid); // Return default on error
        }
    }

    /**
     * Load PlayerData (alias for getPlayerData)
     */
    public synchronized PlayerData load(UUID uuid) {
        return getPlayerData(uuid);
    }

    /**
     * Save PlayerData to disk
     */
    public synchronized void save(PlayerData pd) {
        if (pd == null) {
            plugin.getLogger().warning("Attempted to save null PlayerData");
            return;
        }

        try {
            // Reload config to prevent overwriting other changes
            playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            // Get or create section
            String path = PLAYERS_PATH + "." + pd.getUuid().toString();
            ConfigurationSection section = playerConfig.getConfigurationSection(path);

            if (section == null) {
                section = playerConfig.createSection(path);
            }

            // Save data
            pd.saveTo(section);

            // Update cache
            playerDataCache.put(pd.getUuid(), pd);

            // Write to disk
            saveConfigToDisk();

            plugin.getLogger().fine("Saved PlayerData for " + pd.getUuid());

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save PlayerData for " + pd.getUuid(), e);
        }
    }

    /**
     * Write configuration to disk with error handling
     */
    private void saveConfigToDisk() {
        try {
            playerConfig.save(playerFile);
            lastSaveTime = System.currentTimeMillis();
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save players.yml to disk", e);

            // Attempt emergency backup
            try {
                File emergency = new File(dataDir, "players_emergency_" + System.currentTimeMillis() + ".yml");
                playerConfig.save(emergency);
                plugin.getLogger().warning("Created emergency backup at: " + emergency.getName());
            } catch (IOException e2) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create emergency backup", e2);
            }
        }
    }

    /**
     * Invalidate cache for a specific player (force reload from disk)
     */
    public synchronized void invalidateCache(UUID uuid) {
        playerDataCache.remove(uuid);
        plugin.getLogger().fine("Invalidated cache for " + uuid);
    }

    /**
     * Flush all cached data to disk
     */
    public synchronized void flushAll() {
        plugin.getLogger().info("Flushing all player data to disk...");

        int count = 0;
        for (PlayerData pd : playerDataCache.values()) {
            if (pd.isDirty()) {
                save(pd);
                count++;
            }
        }

        plugin.getLogger().info("Flushed " + count + " player(s) to disk");
    }

    // ========================================
    // BACKUP SYSTEM
    // ========================================

    /**
     * Create a backup of the player data file
     */
    public synchronized boolean createBackup() {
        try {
            // Create backup filename with timestamp
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")
                    .format(new Date());
            File backup = new File(backupDir, "players_" + timestamp + ".yml");

            // Copy file
            java.nio.file.Files.copy(
                    playerFile.toPath(),
                    backup.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            plugin.getLogger().info("Created backup: " + backup.getName());

            // Clean up old backups
            cleanupOldBackups();

            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create backup", e);
            return false;
        }
    }

    /**
     * Keep only the most recent backups
     */
    private void cleanupOldBackups() {
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("players_") && name.endsWith(".yml"));

        if (backups == null || backups.length <= MAX_BACKUPS) {
            return;
        }

        // Sort by modification time (oldest first)
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified));

        // Delete oldest backups
        int toDelete = backups.length - MAX_BACKUPS;
        for (int i = 0; i < toDelete; i++) {
            if (backups[i].delete()) {
                plugin.getLogger().fine("Deleted old backup: " + backups[i].getName());
            }
        }
    }

    /**
     * Recover from the most recent backup
     */
    private boolean recoverFromBackup() {
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("players_") && name.endsWith(".yml"));

        if (backups == null || backups.length == 0) {
            plugin.getLogger().warning("No backups found for recovery");
            return false;
        }

        // Sort by modification time (newest first)
        Arrays.sort(backups, Comparator.comparingLong(File::lastModified).reversed());

        try {
            File mostRecent = backups[0];
            java.nio.file.Files.copy(
                    mostRecent.toPath(),
                    playerFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            // Reload configuration
            this.playerConfig = YamlConfiguration.loadConfiguration(playerFile);

            plugin.getLogger().info("Recovered from backup: " + mostRecent.getName());
            return true;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to recover from backup", e);
            return false;
        }
    }

    // ========================================
    // TRUST SYSTEM (Backwards Compatibility)
    // ========================================

    public synchronized Set<UUID> getTrusted(UUID owner) {
        PlayerData pd = getPlayerData(owner);
        return pd.getTrustedPlayers();
    }

    public synchronized void setTrusted(UUID owner, Set<UUID> trusted) {
        PlayerData pd = getPlayerData(owner);
        pd.setTrustedPlayers(trusted);
        save(pd);
    }

    // ========================================
    // STATISTICS & DEBUGGING
    // ========================================

    /**
     * Get statistics about the data store
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("cached_players", playerDataCache.size());
        stats.put("dirty_players", playerDataCache.values().stream().filter(PlayerData::isDirty).count());
        stats.put("last_save", new Date(lastSaveTime));
        stats.put("player_file_size", playerFile.length());
        stats.put("backup_count", backupDir.listFiles((dir, name) -> name.endsWith(".yml")).length);
        return stats;
    }

    /**
     * Get all cached UUIDs
     */
    public Set<UUID> getCachedPlayers() {
        return new HashSet<>(playerDataCache.keySet());
    }
}