package hs.elementSmpUtility.storage;

import hs.elementSmpUtility.blocks.CustomBlockManager;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages storage and retrieval of custom block data with performance optimizations
 */
public class BlockDataStorage {

    private final JavaPlugin plugin;
    private final CustomBlockManager blockManager;
    private final NamespacedKey storageKey;

    // In-memory cache for performance
    private final Map<String, String> blockCache;

    public BlockDataStorage(JavaPlugin plugin, CustomBlockManager blockManager) {
        this.plugin = plugin;
        this.blockManager = blockManager;
        this.storageKey = new NamespacedKey(plugin, "custom_blocks");
        this.blockCache = new HashMap<>();
    }

    /**
     * Save custom block data when placed
     */
    public void saveCustomBlock(Block block, String blockId) {
        String locationKey = getLocationKey(block.getLocation());
        blockCache.put(locationKey, blockId);

        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();

        // Get existing data
        String existingData = pdc.getOrDefault(storageKey, PersistentDataType.STRING, "");

        // Add new block entry
        String newEntry = locationKey + ":" + blockId;
        String updatedData = existingData.isEmpty() ? newEntry : existingData + ";" + newEntry;

        pdc.set(storageKey, PersistentDataType.STRING, updatedData);
    }

    /**
     * Get custom block ID at a location (FAST - cache only, use for scanning)
     */
    public String getCustomBlockIdCached(Location location) {
        String locationKey = getLocationKey(location);
        return blockCache.get(locationKey);
    }

    /**
     * Get custom block ID at a location (SLOW - loads from PDC if not cached)
     */
    public String getCustomBlockId(Block block) {
        String locationKey = getLocationKey(block.getLocation());

        // Check cache first
        if (blockCache.containsKey(locationKey)) {
            return blockCache.get(locationKey);
        }

        // Load from chunk persistent data only if not in cache
        // This is expensive and should be avoided in loops
        loadSingleBlock(block.getChunk(), locationKey);
        return blockCache.get(locationKey);
    }

    /**
     * Load a single block from chunk data (expensive operation)
     */
    private void loadSingleBlock(Chunk chunk, String locationKey) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(storageKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return;
        }

        // Parse data to find block
        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2 && parts[0].equals(locationKey)) {
                blockCache.put(locationKey, parts[1]);
                return;
            }
        }
    }

    /**
     * Remove custom block data when broken
     */
    public void removeCustomBlock(Block block) {
        String locationKey = getLocationKey(block.getLocation());
        blockCache.remove(locationKey);

        Chunk chunk = block.getChunk();
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(storageKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return;
        }

        // Remove block entry
        StringBuilder newData = new StringBuilder();
        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2 && !parts[0].equals(locationKey)) {
                if (newData.length() > 0) {
                    newData.append(";");
                }
                newData.append(entry);
            }
        }

        if (newData.length() == 0) {
            pdc.remove(storageKey);
        } else {
            pdc.set(storageKey, PersistentDataType.STRING, newData.toString());
        }
    }

    /**
     * Load all custom blocks in a chunk into cache
     */
    public void loadChunk(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        String data = pdc.getOrDefault(storageKey, PersistentDataType.STRING, "");

        if (data.isEmpty()) {
            return;
        }

        for (String entry : data.split(";")) {
            if (entry.isEmpty()) continue;

            String[] parts = entry.split(":", 2);
            if (parts.length == 2) {
                blockCache.put(parts[0], parts[1]);
            }
        }
    }

    /**
     * Unload chunk data from cache
     */
    public void unloadChunk(Chunk chunk) {
        String chunkKey = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
        blockCache.entrySet().removeIf(entry -> entry.getKey().startsWith(chunkKey));
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
}