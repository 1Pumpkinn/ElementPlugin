package hs.elementSmpUtility.listeners;

import hs.elementSmpUtility.blocks.custom.PedestalBlock;
import hs.elementSmpUtility.storage.BlockDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalOwnerStorage;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Manages loading and unloading chunk data cache
 * Restores pedestal displays when chunks load
 */
public class ChunkListener implements Listener {

    private final BlockDataStorage storage;
    private final PedestalDataStorage pedestalStorage;
    private final PedestalOwnerStorage ownerStorage;

    public ChunkListener(BlockDataStorage storage, PedestalDataStorage pedestalStorage,
                         PedestalOwnerStorage ownerStorage) {
        this.storage = storage;
        this.pedestalStorage = pedestalStorage;
        this.ownerStorage = ownerStorage;
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();

        // Load data into cache (instant, no lag)
        storage.loadChunk(chunk);
        pedestalStorage.loadChunk(chunk);
        ownerStorage.loadChunk(chunk);

        // Restore pedestal displays asynchronously to avoid lag
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ElementSmpUtility");
        if (plugin != null) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                restorePedestalDisplays(chunk);
            }, 1L); // 1 tick delay to let chunk fully load
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();

        // Unload data from cache to free memory
        storage.unloadChunk(chunk);
        pedestalStorage.unloadChunk(chunk);
        ownerStorage.unloadChunk(chunk);
    }

    /**
     * Restore all pedestal displays in a chunk based on stored data
     * CRITICAL: Also removes any orphaned armor stands at pedestal locations
     */
    private void restorePedestalDisplays(Chunk chunk) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ElementSmpUtility");
        int restored = 0;
        int cleaned = 0;

        // Scan chunk for pedestals
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = chunk.getWorld().getMinHeight(); y < chunk.getWorld().getMaxHeight(); y++) {
                    Block block = chunk.getBlock(x, y, z);

                    // Check if it's a lodestone (pedestal base material)
                    if (block.getType() == Material.LODESTONE) {
                        Location loc = block.getLocation();

                        // Check if it's registered as a pedestal
                        String blockId = storage.getCustomBlockIdCached(loc);
                        if ("pedestal".equals(blockId)) {
                            // FIRST: Remove any existing armor stands (cleanup orphans from restart)
                            PedestalBlock.removeAllDisplays(loc);

                            // THEN: Get stored item and recreate display if needed
                            ItemStack storedItem = pedestalStorage.getPedestalItem(loc);

                            if (storedItem != null && storedItem.getType() != Material.AIR) {
                                // Restore the display
                                PedestalBlock.createOrUpdateDisplay(loc, storedItem);
                                restored++;

                                if (plugin != null) {
                                    plugin.getLogger().info(
                                            "Restored pedestal display at " +
                                                    loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() +
                                                    " with " + storedItem.getType() +
                                                    " (Owner: " + ownerStorage.getOwnerName(loc) + ")"
                                    );
                                }
                            } else {
                                // No item stored, cleanup was already done above
                                cleaned++;
                            }
                        }
                    }
                }
            }
        }

        if (plugin != null && (restored > 0 || cleaned > 0)) {
            plugin.getLogger().info(
                    "Chunk " + chunk.getX() + "," + chunk.getZ() +
                            ": Restored " + restored + " pedestals, cleaned " + cleaned + " empty pedestals"
            );
        }
    }
}