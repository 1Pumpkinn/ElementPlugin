package hs.elementSmpUtility.listeners;

import hs.elementSmpUtility.storage.BlockDataStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.block.BlockExplodeEvent;

import java.util.Iterator;

/**
 * Protects pedestals from being destroyed by explosions (creepers, TNT, etc.)
 */
public class PedestalProtectionListener implements Listener {

    private final BlockDataStorage blockStorage;

    public PedestalProtectionListener(BlockDataStorage blockStorage) {
        this.blockStorage = blockStorage;
    }

    /**
     * Prevent entity explosions (creepers, TNT, etc.) from destroying pedestals
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Remove pedestals from the explosion block list
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            String blockId = blockStorage.getCustomBlockIdCached(block.getLocation());

            if ("pedestal".equals(blockId)) {
                iterator.remove();
            }
        }
    }

    /**
     * Prevent block explosions (respawn anchors, beds in wrong dimension, etc.) from destroying pedestals
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (event.isCancelled()) {
            return;
        }

        // Remove pedestals from the explosion block list
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            String blockId = blockStorage.getCustomBlockIdCached(block.getLocation());

            if ("pedestal".equals(blockId)) {
                iterator.remove();
            }
        }
    }
}