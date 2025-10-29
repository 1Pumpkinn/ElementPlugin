package hs.elementSmpUtility.listeners.block;

import hs.elementSmpUtility.blocks.CustomBlockManager;
import hs.elementSmpUtility.blocks.CustomBlockType;
import hs.elementSmpUtility.blocks.custom.PedestalBlock;
import hs.elementSmpUtility.storage.BlockDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalOwnerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Handles breaking custom blocks and preventing unbreakable blocks from being broken
 */
public class BlockBreakListener implements Listener {

    private final CustomBlockManager blockManager;
    private final BlockDataStorage storage;
    private final PedestalDataStorage pedestalStorage;
    private final PedestalOwnerStorage ownerStorage;

    public BlockBreakListener(CustomBlockManager blockManager, BlockDataStorage storage,
                              PedestalDataStorage pedestalStorage, PedestalOwnerStorage ownerStorage) {
        this.blockManager = blockManager;
        this.storage = storage;
        this.pedestalStorage = pedestalStorage;
        this.ownerStorage = ownerStorage;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        String blockId = storage.getCustomBlockId(event.getBlock());
        if (blockId == null) {
            return;
        }

        CustomBlockType blockType = blockManager.getBlockType(blockId);
        if (blockType == null) {
            return;
        }

        Player player = event.getPlayer();
        Location location = event.getBlock().getLocation();

        // Handle pedestal breaking - CHECK OWNERSHIP FIRST
        if ("pedestal".equals(blockId)) {
            UUID ownerUUID = ownerStorage.getOwner(location);

            // If no owner, only admins can break (to claim or fix)
            if (ownerUUID == null) {
                if (!player.hasPermission("elementsmp.pedestal.admin")) {
                    event.setCancelled(true);
                    player.sendActionBar(
                            Component.text("This pedestal has no owner! Ask an admin to claim it.")
                                    .color(TextColor.color(0xFF5555))
                    );
                    return;
                }
                // Admin can break unclaimed pedestals - allow drop
                handlePedestalBreak(event, true);
                storage.removeCustomBlock(event.getBlock());
                return;
            }

            // Check ownership
            boolean isOwner = ownerUUID.equals(player.getUniqueId());
            boolean hasAdmin = player.hasPermission("elementsmp.pedestal.admin");

            // If not owner and not admin, completely deny breaking
            if (!isOwner && !hasAdmin) {
                event.setCancelled(true);
                String ownerName = ownerStorage.getOwnerName(location);
                player.sendActionBar(
                        Component.text("This pedestal belongs to " + ownerName + "!")
                                .color(TextColor.color(0xFF5555))
                );
                return;
            }

            // Ownership check passed
            // Owner gets the pedestal back, admin breaks it without drop
            handlePedestalBreak(event, isOwner);
            storage.removeCustomBlock(event.getBlock());
            return;
        }

        // Handle other custom blocks - check if unbreakable
        if (blockType.isUnbreakable()) {
            // Allow creative mode players to break
            if (player.getGameMode() == GameMode.CREATIVE) {
                storage.removeCustomBlock(event.getBlock());
                return;
            }

            // Cancel break for survival players
            event.setCancelled(true);
            player.sendActionBar(
                    Component.text("This block is unbreakable!")
                            .color(TextColor.color(0xFF5555))
            );
            return;
        }

        // If breakable, remove from storage
        storage.removeCustomBlock(event.getBlock());
    }

    /**
     * Handle breaking a pedestal block - properly cleans up ALL armor stands
     * and drops the correct pedestal item (only if shouldDrop is true)
     *
     * @param event The block break event
     * @param shouldDrop Whether the pedestal item should drop (true for owner, false for admin)
     */
    private void handlePedestalBreak(BlockBreakEvent event, boolean shouldDrop) {
        Location loc = event.getBlock().getLocation();

        // CRITICAL: Prevent the default lodestone drop
        event.setDropItems(false);

        // Get displayed item from storage
        ItemStack displayedItem = pedestalStorage.getPedestalItem(loc);

        // Remove ALL nearby armor stands
        PedestalBlock.removeAllDisplays(loc);

        // Verify removal
        ArmorStand remaining = PedestalBlock.getExistingDisplay(loc);
        if (remaining != null) {
            blockManager.getPlugin().getLogger().warning(
                    "Armor stand still exists after removal attempt! Force removing..."
            );
            remaining.remove();
        }

        // Only drop the pedestal item if shouldDrop is true (owner or admin with unclaimed)
        if (shouldDrop) {
            ItemStack pedestalItem = blockManager.createCustomBlock("pedestal", 1);
            if (pedestalItem != null) {
                event.getBlock().getWorld().dropItemNaturally(loc, pedestalItem);
            } else {
                blockManager.getPlugin().getLogger().warning(
                        "Failed to create pedestal item for drop at " + loc
                );
            }
        }

        // Always drop the displayed item if present (belongs to the owner)
        if (displayedItem != null && displayedItem.getType() != Material.AIR) {
            event.getBlock().getWorld().dropItemNaturally(loc, displayedItem);
        }

        // Remove from pedestal storage
        pedestalStorage.savePedestalItem(loc, null);

        // Remove owner data
        ownerStorage.removeOwner(loc);
    }
}