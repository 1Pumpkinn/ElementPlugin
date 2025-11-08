package hs.elementPlugin.data;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DataStore {
    private final ElementPlugin plugin;

    private final File playerFile;
    private FileConfiguration playerCfg;
    private final File serverFile;
    private final FileConfiguration serverCfg;

    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    public PlayerData getPlayerData(UUID uuid) {
        // Check cache first
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }

        // Load from file if not in cache
        PlayerData data = loadPlayerDataFromFile(uuid);
        playerDataCache.put(uuid, data);
        return data;
    }

    private PlayerData loadPlayerDataFromFile(UUID uuid) {
        // CRITICAL FIX: Reload the config from disk to ensure we have the latest data
        try {
            playerCfg = YamlConfiguration.loadConfiguration(playerFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload player configuration", e);
        }

        String uuidString = uuid.toString();
        ConfigurationSection section = null;

        // Try BOTH formats: "players.<uuid>" AND just "<uuid>" at root level
        // First try the standard format with "players" prefix
        section = playerCfg.getConfigurationSection("players." + uuidString);

        // If not found, try root level (legacy format)
        if (section == null) {
            section = playerCfg.getConfigurationSection(uuidString);
            if (section != null) {
                plugin.getLogger().info("[DataStore] Found data for " + uuid + " at ROOT level (legacy format)");
            }
        } else {
            plugin.getLogger().info("[DataStore] Found data for " + uuid + " under 'players' section");
        }

        if (section == null) {
            plugin.getLogger().warning("[DataStore] No data found for " + uuid + " - creating new PlayerData");
            return new PlayerData(uuid);
        }

        plugin.getLogger().info("[DataStore] Loading data for " + uuid + " from file");
        PlayerData data = new PlayerData(uuid, section);

        // Log what was loaded
        plugin.getLogger().info("[DataStore] Loaded element: " +
                (data.getCurrentElement() != null ? data.getCurrentElement().name() : "null"));
        plugin.getLogger().info("[DataStore] Loaded mana: " + data.getMana());
        plugin.getLogger().info("[DataStore] Loaded upgrade level: " + data.getCurrentElementUpgradeLevel());

        return data;
    }

    public DataStore(ElementPlugin plugin) {
        this.plugin = plugin;
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            if (!dataDir.mkdirs()) {
                plugin.getLogger().severe("Failed to create data directory: " + dataDir.getAbsolutePath());
                throw new RuntimeException("Could not create data directory");
            }
        }

        this.playerFile = new File(dataDir, "players.yml");
        if (!playerFile.exists()) {
            try {
                if (!playerFile.createNewFile()) {
                    plugin.getLogger().severe("Failed to create players.yml file");
                    throw new RuntimeException("Could not create players.yml file");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating players.yml: " + e.getMessage());
                throw new RuntimeException("Could not create players.yml file", e);
            }
        }

        try {
            this.playerCfg = YamlConfiguration.loadConfiguration(playerFile);

            // Count players in BOTH formats
            int playersCount = 0;
            ConfigurationSection playersSection = playerCfg.getConfigurationSection("players");
            if (playersSection != null) {
                playersCount = playersSection.getKeys(false).size();
            }

            // Also count root-level UUIDs (legacy format)
            int rootLevelCount = 0;
            for (String key : playerCfg.getKeys(false)) {
                if (!key.equals("players")) {
                    try {
                        UUID.fromString(key);
                        rootLevelCount++;
                    } catch (IllegalArgumentException e) {
                        // Not a UUID, skip
                    }
                }
            }

            plugin.getLogger().info("[DataStore] Loaded players.yml with " + playersCount +
                    " players (under 'players' section) + " + rootLevelCount +
                    " players (root level/legacy format) = " + (playersCount + rootLevelCount) + " total");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load players.yml configuration: " + e.getMessage());
            throw new RuntimeException("Could not load players.yml configuration", e);
        }

        this.serverFile = new File(dataDir, "server.yml");
        if (!serverFile.exists()) {
            try {
                if (!serverFile.createNewFile()) {
                    plugin.getLogger().severe("Failed to create server.yml file");
                    throw new RuntimeException("Could not create server.yml file");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Error creating server.yml: " + e.getMessage());
                throw new RuntimeException("Could not create server.yml file", e);
            }
        }

        try {
            this.serverCfg = YamlConfiguration.loadConfiguration(serverFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load server.yml configuration: " + e.getMessage());
            throw new RuntimeException("Could not load server.yml configuration", e);
        }
    }

    public synchronized PlayerData load(UUID uuid) {
        return getPlayerData(uuid);
    }

    public synchronized void save(PlayerData pd) {
        try {
            // CRITICAL FIX: Reload config before saving to avoid overwriting other changes
            playerCfg = YamlConfiguration.loadConfiguration(playerFile);

            String key = "players." + pd.getUuid().toString();
            ConfigurationSection sec = playerCfg.getConfigurationSection(key);
            if (sec == null) sec = playerCfg.createSection(key);

            sec.set("element", pd.getCurrentElement() == null ? null : pd.getCurrentElement().name());
            sec.set("mana", pd.getMana());
            sec.set("currentUpgradeLevel", pd.getCurrentElementUpgradeLevel());

            java.util.List<String> items = new java.util.ArrayList<>();
            for (ElementType t : pd.getOwnedItems()) items.add(t.name());
            sec.set("items", items);

            // Update cache
            playerDataCache.put(pd.getUuid(), pd);

            // Save to disk
            flushPlayerData();

            plugin.getLogger().info("[DataStore] Saved data for " + pd.getUuid() +
                    " - Element: " + (pd.getCurrentElement() != null ? pd.getCurrentElement().name() : "null") +
                    ", Mana: " + pd.getMana() +
                    ", Upgrade: " + pd.getCurrentElementUpgradeLevel());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + pd.getUuid(), e);
        }
    }

    public synchronized void invalidateCache(UUID uuid) {
        playerDataCache.remove(uuid);
        plugin.getLogger().info("[DataStore] Invalidated cache for " + uuid);
    }

    // NEW METHOD: Debug method to check cache state
    public void debugCacheState(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            PlayerData cached = playerDataCache.get(uuid);
            plugin.getLogger().info("[DataStore] DEBUG - Cached element for " + uuid + ": " +
                    (cached.getCurrentElement() != null ? cached.getCurrentElement().name() : "null"));
        } else {
            plugin.getLogger().info("[DataStore] DEBUG - No cached data for " + uuid);
        }

        // Check BOTH file locations
        ConfigurationSection section = playerCfg.getConfigurationSection("players." + uuid.toString());
        if (section != null) {
            String fileElement = section.getString("element");
            plugin.getLogger().info("[DataStore] DEBUG - File element for " + uuid + " (under 'players'): " + fileElement);
        } else {
            // Try root level
            section = playerCfg.getConfigurationSection(uuid.toString());
            if (section != null) {
                String fileElement = section.getString("element");
                plugin.getLogger().info("[DataStore] DEBUG - File element for " + uuid + " (root level): " + fileElement);
            } else {
                plugin.getLogger().info("[DataStore] DEBUG - No file data found for " + uuid);
            }
        }
    }

    public synchronized void flushAll() {
        flushPlayerData();
    }

    private void flushPlayerData() {
        try {
            playerCfg.save(playerFile);
            plugin.getLogger().info("[DataStore] Flushed player data to disk");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save players.yml to disk", e);
        }
    }

    // TRUST store
    public synchronized Set<UUID> getTrusted(UUID owner) {
        try {
            // CRITICAL FIX: Reload config to get latest trust data
            playerCfg = YamlConfiguration.loadConfiguration(playerFile);

            ConfigurationSection sec = playerCfg.getConfigurationSection("players." + owner + ".trust");
            Set<UUID> set = new HashSet<>();
            if (sec != null) {
                for (String k : sec.getKeys(false)) {
                    try {
                        set.add(UUID.fromString(k));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in trust data: " + k + " for player " + owner);
                    }
                }
            }
            return set;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load trust data for " + owner, e);
            return new HashSet<>();
        }
    }

    public synchronized void setTrusted(UUID owner, Set<UUID> trusted) {
        try {
            // CRITICAL FIX: Reload config before modifying
            playerCfg = YamlConfiguration.loadConfiguration(playerFile);

            String base = "players." + owner + ".trust";
            playerCfg.set(base, null);
            ConfigurationSection sec = playerCfg.createSection(base);
            for (UUID u : trusted) sec.set(u.toString(), true);
            flushPlayerData();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save trust data for " + owner, e);
        }
    }

    // Server-wide restrictions
    public synchronized boolean isLifeElementCrafted() {
        try {
            return serverCfg.getBoolean("life_crafted", false);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check life element crafted status", e);
            return false;
        }
    }

    public synchronized void setLifeElementCrafted(boolean crafted) {
        try {
            serverCfg.set("life_crafted", crafted);
            flushServerData();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set life element crafted status", e);
        }
    }

    public synchronized boolean isDeathElementCrafted() {
        try {
            return serverCfg.getBoolean("death_crafted", false);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check death element crafted status", e);
            return false;
        }
    }

    public synchronized void setDeathElementCrafted(boolean crafted) {
        try {
            serverCfg.set("death_crafted", crafted);
            flushServerData();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set death element crafted status", e);
        }
    }

    private void flushServerData() {
        try {
            serverCfg.save(serverFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save server.yml to disk", e);
        }
    }
}