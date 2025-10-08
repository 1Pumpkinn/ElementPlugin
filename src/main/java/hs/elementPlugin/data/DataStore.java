package hs.elementPlugin.data;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.OfflinePlayer;
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
    private final FileConfiguration playerCfg;
    private final File serverFile;
    private final FileConfiguration serverCfg;
    
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();
    
    public PlayerData getPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) {
            return playerDataCache.get(uuid);
        }
        
        PlayerData data = loadPlayerData(uuid);
        playerDataCache.put(uuid, data);
        return data;
    }
    
    private PlayerData loadPlayerData(UUID uuid) {
        ConfigurationSection section = playerCfg.getConfigurationSection("players." + uuid.toString());
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
        try {
            String key = uuid.toString();
            ConfigurationSection sec = playerCfg.getConfigurationSection(key);
            PlayerData pd = new PlayerData(uuid);
            if (sec != null) {
                String elem = sec.getString("element");
                if (elem != null) {
                    try {
                        pd.setCurrentElementWithoutReset(ElementType.valueOf(elem));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid element type '" + elem + "' for player " + uuid + ", ignoring");
                    }
                }
                pd.setMana(sec.getInt("mana", 100));
                pd.setCurrentElementUpgradeLevel(sec.getInt("currentUpgradeLevel", 0));
                java.util.List<String> items = sec.getStringList("items");
                if (items != null) {
                    for (String name : items) {
                        try {
                            ElementType t = ElementType.valueOf(name);
                            pd.addElementItem(t);
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("Invalid element item '" + name + "' for player " + uuid + ", skipping");
                        }
                    }
                }
            }
            return pd;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
            // Return default player data to prevent crashes
            return new PlayerData(uuid);
        }
    }

    public synchronized void save(PlayerData pd) {
        try {
            String key = pd.getUuid().toString();
            ConfigurationSection sec = playerCfg.getConfigurationSection(key);
            if (sec == null) sec = playerCfg.createSection(key);
            sec.set("element", pd.getCurrentElement() == null ? null : pd.getCurrentElement().name());
            sec.set("mana", pd.getMana());
            sec.set("currentUpgradeLevel", pd.getCurrentElementUpgradeLevel());
            java.util.List<String> items = new java.util.ArrayList<>();
            for (ElementType t : pd.getOwnedItems()) items.add(t.name());
            sec.set("items", items);
            flushPlayerData();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + pd.getUuid(), e);
        }
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

    // TRUST store
    public synchronized Set<UUID> getTrusted(UUID owner) {
        try {
            ConfigurationSection sec = playerCfg.getConfigurationSection(owner + ".trust");
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
            return new HashSet<>(); // Return empty set to prevent crashes
        }
    }

    public synchronized void setTrusted(UUID owner, Set<UUID> trusted) {
        try {
            String base = owner + ".trust";
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
            return false; // Safe default
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

    private void flushServerData() {
        try { 
            serverCfg.save(serverFile); 
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save server.yml to disk", e);
        }
    }
}