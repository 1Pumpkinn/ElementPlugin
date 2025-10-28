package hs.elementSmpUtility.storage.pedestal;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages pedestal ownership using DUAL storage: Chunk PDC + YAML backup
 */
public class PedestalOwnerStorage {

    private final JavaPlugin plugin;
    private final NamespacedKey ownerKey;
    private final Map<String, UUID> ownerCache;
    private final File ownerFile;
    private FileConfiguration ownerConfig;

    public PedestalOwnerStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "pedestal_owners");
        this.ownerCache = new HashMap<>();
        this.ownerFile = new File(plugin.getDataFolder(), "pedestal_owners.yml");

        loadOwnerConfig();
        plugin.getLogger().info("Pedestal owner storage initialized");
    }

    private void loadOwnerConfig() {
        if (!ownerFile.exists()) {
            try {
                ownerFile.getParentFile().mkdirs();
                ownerFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create pedestal_owners.yml: " + e.getMessage());
            }
        }
        ownerConfig = YamlConfiguration.loadConfiguration(ownerFile);
    }

    /**
     * Set the owner of a pedestal (DUAL SAVE: PDC + YAML)
     */
    public void setOwner(Location location, UUID ownerUUID) {
        String locationKey = getLocationKey(location);

        if (ownerUUID == null) {
            ownerCache.remove(locationKey);
            removeFromChunkPDC(location);
            removeFromYAML(location);
            return;
        }

        // Save to cache
        ownerCache.put(locationKey, ownerUUID);

        // Save to Chunk PDC
        saveToChunkPDC(location, ownerUUID);

        // Save to YAML backup
        saveToYAML(location, ownerUUID);
    }

    /**
     * Get the owner UUID of a pedestal (tries cache → PDC → YAML)
     */
    public UUID getOwner(Location location) {
        String locationKey = getLocationKey(location);

        // Try cache first
        if (ownerCache.containsKey(locationKey)) {
            return ownerCache.get(locationKey);
        }

        // Try PDC
        UUID fromPDC = loadFromChunkPDC(location);
        if (fromPDC != null) {
            ownerCache.put(locationKey, fromPDC);
            return fromPDC;
        }

        // Try YAML backup
        UUID fromYAML = loadFromYAML(location);
        if (fromYAML != null) {
            ownerCache.put(locationKey, fromYAML);
        }

        return fromYAML;
    }

    /**
     * Check if a player is the owner of a pedestal
     */
    public boolean isOwner(Location location, UUID playerUUID) {
        UUID owner = getOwner(location);
        return owner != null && owner.equals(playerUUID);
    }

    /**
     * Remove owner data for a pedestal
     */
    public void removeOwner(Location location) {
        setOwner(location, null);
    }

    /**
     * Save to Chunk PDC
     */
    private void saveToChunkPDC(Location location, UUID ownerUUID) {
        String locationKey = getLocationKey(location);
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        String existingData = pdc.getOrDefault(ownerKey, PersistentDataType.STRING, "");
        String newEntry = locationKey + ":" + ownerUUID.toString();

        StringBuilder newData = new StringBuilder();
        boolean found = false;

        if (!existingData.isEmpty()) {
            for (String entry : existingData.split(";")) {
                if (entry.isEmpty()) continue;

                String[] parts = entry.split(":", 2);
                if (parts.length == 2) {
                    if (parts[0].equals(locationKey)) {
                        newData.append(newEntry);
                        found = true;
                    } else {
                        newData.append(entry);
                    }
                    newData.append(";");
                }
            }
        }

        if (!found) {
            newData.append(newEntry).append(";");
        }

        pdc.set(ownerKey, PersistentDataType.STRING, newData.toString());
    }

    /**
     * Load from Chunk PDC
     */
    private UUID loadFromChunkPDC(Location location) {
        String locationKey = getLocationKey(location);
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(ownerKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return null;
        }

        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2 && parts[0].equals(locationKey)) {
                try {
                    return UUID.fromString(parts[1]);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in pedestal owner data: " + parts[1]);
                }
            }
        }

        return null;
    }

    /**
     * Remove from Chunk PDC
     */
    private void removeFromChunkPDC(Location location) {
        String locationKey = getLocationKey(location);
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(ownerKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return;
        }

        StringBuilder newData = new StringBuilder();
        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2 && !parts[0].equals(locationKey)) {
                newData.append(entry).append(";");
            }
        }

        if (newData.length() == 0) {
            pdc.remove(ownerKey);
        } else {
            pdc.set(ownerKey, PersistentDataType.STRING, newData.toString());
        }
    }

    /**
     * Save to YAML
     */
    private void saveToYAML(Location location, UUID ownerUUID) {
        String key = getLocationKey(location);
        ownerConfig.set(key, ownerUUID.toString());
        saveYAMLConfig();
    }

    /**
     * Load from YAML
     */
    private UUID loadFromYAML(Location location) {
        String key = getLocationKey(location);
        String uuidString = ownerConfig.getString(key);

        if (uuidString != null) {
            try {
                return UUID.fromString(uuidString);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in YAML: " + uuidString);
            }
        }

        return null;
    }

    /**
     * Remove from YAML
     */
    private void removeFromYAML(Location location) {
        String key = getLocationKey(location);
        ownerConfig.set(key, null);
        saveYAMLConfig();
    }

    /**
     * Save YAML config
     */
    private void saveYAMLConfig() {
        try {
            ownerConfig.save(ownerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pedestal_owners.yml: " + e.getMessage());
        }
    }

    /**
     * Load chunk data into cache
     */
    public void loadChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(ownerKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return;
        }

        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                try {
                    UUID owner = UUID.fromString(parts[1]);
                    ownerCache.put(parts[0], owner);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in chunk data: " + parts[1]);
                }
            }
        }
    }

    /**
     * Unload chunk data from cache
     */
    public void unloadChunk(Chunk chunk) {
        String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
        ownerCache.entrySet().removeIf(entry -> entry.getKey().startsWith(chunkKey));
    }

    /**
     * Create location key
     */
    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "," +
                loc.getBlockX() + "," +
                loc.getBlockY() + "," +
                loc.getBlockZ();
    }

    /**
     * Get owner name (for display purposes)
     */
    public String getOwnerName(Location location) {
        UUID owner = getOwner(location);
        if (owner == null) {
            return "Unknown";
        }

        return Bukkit.getOfflinePlayer(owner).getName();
    }
}