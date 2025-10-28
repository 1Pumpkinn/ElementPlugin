package hs.elementSmpUtility.storage.pedestal;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages storage of pedestal item data
 * Uses DUAL storage: Chunk PDC (primary) + YAML file (backup)
 */
public class PedestalDataStorage {

    private final JavaPlugin plugin;
    private final NamespacedKey pedestalKey;
    private final Map<String, ItemStack> pedestalCache;
    private final PedestalYmlStorage ymlStorage;

    public PedestalDataStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.pedestalKey = new NamespacedKey(plugin, "pedestal_items");
        this.pedestalCache = new HashMap<>();
        this.ymlStorage = new PedestalYmlStorage(plugin);

        // Log storage initialization
        plugin.getLogger().info("Pedestal storage initialized (PDC + YAML backup)");
        plugin.getLogger().info("Found " + ymlStorage.getPedestalCount() + " pedestals in YAML backup");
    }

    /**
     * Save item data for a pedestal (DUAL SAVE: PDC + YAML)
     */
    public void savePedestalItem(Location location, ItemStack item) {
        String locationKey = getLocationKey(location);

        if (item == null || item.getType().isAir()) {
            pedestalCache.remove(locationKey);
            removePedestalFromChunk(location);
            ymlStorage.removePedestal(location);
            return;
        }

        // Save to cache
        pedestalCache.put(locationKey, item.clone());

        // Save to Chunk PDC
        saveToChunkPDC(location, item);

        // Save to YAML backup
        ymlStorage.savePedestal(location, item);
    }

    /**
     * Save to Chunk PDC
     */
    private void saveToChunkPDC(Location location, ItemStack item) {
        String locationKey = getLocationKey(location);
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        String existingData = pdc.getOrDefault(pedestalKey, PersistentDataType.STRING, "");
        String serializedItem = serializeItem(item);
        String newEntry = locationKey + ":" + serializedItem;

        // Update or add entry
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

        pdc.set(pedestalKey, PersistentDataType.STRING, newData.toString());
    }

    /**
     * Get item data for a pedestal (tries cache → PDC → YAML)
     */
    public ItemStack getPedestalItem(Location location) {
        String locationKey = getLocationKey(location);

        // Try cache first
        if (pedestalCache.containsKey(locationKey)) {
            return pedestalCache.get(locationKey);
        }

        // Try PDC
        ItemStack fromPDC = loadFromChunkPDC(location);
        if (fromPDC != null) {
            pedestalCache.put(locationKey, fromPDC);
            return fromPDC;
        }

        // Try YAML backup as last resort
        ItemStack fromYML = ymlStorage.loadPedestal(location);
        if (fromYML != null) {
            pedestalCache.put(locationKey, fromYML);
            plugin.getLogger().info("Restored pedestal from YAML backup at " +
                    location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ());
        }

        return fromYML;
    }

    /**
     * Load from Chunk PDC
     */
    private ItemStack loadFromChunkPDC(Location location) {
        String locationKey = getLocationKey(location);
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(pedestalKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return null;
        }

        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2 && parts[0].equals(locationKey)) {
                return deserializeItem(parts[1]);
            }
        }

        return null;
    }

    /**
     * Remove pedestal data
     */
    private void removePedestalFromChunk(Location location) {
        String locationKey = getLocationKey(location);
        Chunk chunk = location.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(pedestalKey, PersistentDataType.STRING, "");

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
            pdc.remove(pedestalKey);
        } else {
            pdc.set(pedestalKey, PersistentDataType.STRING, newData.toString());
        }
    }

    /**
     * Load chunk data into cache (also syncs with YAML)
     */
    public void loadChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(pedestalKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return;
        }

        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                ItemStack item = deserializeItem(parts[1]);
                if (item != null) {
                    pedestalCache.put(parts[0], item);
                }
            }
        }
    }

    /**
     * Unload chunk data from cache
     */
    public void unloadChunk(Chunk chunk) {
        String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
        pedestalCache.entrySet().removeIf(entry -> entry.getKey().startsWith(chunkKey));
    }

    /**
     * Serialize ItemStack to Base64 string
     */
    private String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to serialize item: " + e.getMessage());
            return "";
        }
    }

    /**
     * Deserialize ItemStack from Base64 string
     */
    private ItemStack deserializeItem(String data) {
        try {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
            BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
            ItemStack item = (ItemStack) dataInput.readObject();
            dataInput.close();
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize item: " + e.getMessage());
            return null;
        }
    }

    /**
     * Create a unique location key
     */
    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "," +
                loc.getBlockX() + "," +
                loc.getBlockY() + "," +
                loc.getBlockZ();
    }

    /**
     * Get YAML storage for direct access
     */
    public PedestalYmlStorage getYmlStorage() {
        return ymlStorage;
    }
}