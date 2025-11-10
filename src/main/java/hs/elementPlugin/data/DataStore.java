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
        try {
            playerCfg = YamlConfiguration.loadConfiguration(playerFile);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload player configuration", e);
            return new PlayerData(uuid);
        }

        String uuidString = uuid.toString();
        ConfigurationSection section = null;

        // CRITICAL: Try "players.<uuid>" format FIRST (this is the primary location)
        section = playerCfg.getConfigurationSection("players." + uuidString);

        // Fallback: Try root level (legacy format)
        if (section == null) {
            section = playerCfg.getConfigurationSection(uuidString);

            // If found at root level, log a warning
            if (section != null) {
                plugin.getLogger().warning("Found player data for " + uuidString + " at root level. Consider migrating to 'players.' format.");
            }
        }

        if (section == null) {
            return new PlayerData(uuid);
        }

        return new PlayerData(uuid, section);
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
            playerCfg = YamlConfiguration.loadConfiguration(playerFile);

            // ALWAYS use "players.<uuid>" format for consistency
            String key = "players." + pd.getUuid().toString();
            ConfigurationSection sec = playerCfg.getConfigurationSection(key);
            if (sec == null) sec = playerCfg.createSection(key);

            sec.set("element", pd.getCurrentElement() == null ? null : pd.getCurrentElement().name());
            sec.set("mana", pd.getMana());
            sec.set("currentUpgradeLevel", pd.getCurrentElementUpgradeLevel());

            java.util.List<String> items = new java.util.ArrayList<>();
            for (ElementType t : pd.getOwnedItems()) items.add(t.name());
            sec.set("items", items);

            // Save trust list
            sec.set("trust", null); // Clear existing
            if (!pd.getTrustedPlayers().isEmpty()) {
                ConfigurationSection trustSec = sec.createSection("trust");
                for (UUID trustedUuid : pd.getTrustedPlayers()) {
                    trustSec.set(trustedUuid.toString(), true);
                }
            }

            // Update cache
            playerDataCache.put(pd.getUuid(), pd);

            // Save to disk
            flushPlayerData();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + pd.getUuid(), e);
        }
    }

    public synchronized void invalidateCache(UUID uuid) {
        playerDataCache.remove(uuid);
    }

    public synchronized void flushAll() {
        flushPlayerData();
    }

    private void flushPlayerData() {
        try {
            playerCfg.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save players.yml to disk", e);
        }
    }

    // TRUST store - now delegates to PlayerData
    public synchronized Set<UUID> getTrusted(UUID owner) {
        PlayerData pd = getPlayerData(owner);
        return pd.getTrustedPlayers();
    }

    public synchronized void setTrusted(UUID owner, Set<UUID> trusted) {
        PlayerData pd = getPlayerData(owner);
        pd.setTrustedPlayers(trusted);
        save(pd);
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