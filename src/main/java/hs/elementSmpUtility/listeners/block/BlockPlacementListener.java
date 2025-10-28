package hs.elementSmpUtility.listeners.block;

import hs.elementSmpUtility.blocks.CustomBlockManager;
import hs.elementSmpUtility.blocks.custom.PedestalBlock;
import hs.elementSmpUtility.storage.BlockDataStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * Handles placing custom blocks and storing their data
 */
public class BlockPlacementListener implements Listener {

    private final CustomBlockManager blockManager;
    private final BlockDataStorage storage;
    private final hs.elementSmpUtility.storage.pedestal.PedestalOwnerStorage ownerStorage;

    public BlockPlacementListener(CustomBlockManager blockManager, BlockDataStorage storage,
                                  hs.elementSmpUtility.storage.pedestal.PedestalOwnerStorage ownerStorage) {
        this.blockManager = blockManager;
        this.storage = storage;
        this.ownerStorage = ownerStorage;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String blockId = blockManager.getCustomBlockId(event.getItemInHand());
        if (blockId == null) {
            return;
        }

        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();

        // Store custom block data
        storage.saveCustomBlock(block, blockId);

        // If it's a pedestal, create the display armor stand and set owner
        if ("pedestal".equals(blockId)) {
            PedestalBlock.createOrUpdateDisplay(block.getLocation(), null);

            // Set the player as the owner
            ownerStorage.setOwner(block.getLocation(), player.getUniqueId());

            // Notify player
            player.sendActionBar(
                    Component.text("Pedestal claimed! Only you can modify it.")
                            .color(TextColor.color(0x55FF55))
            );
        }
    }
}