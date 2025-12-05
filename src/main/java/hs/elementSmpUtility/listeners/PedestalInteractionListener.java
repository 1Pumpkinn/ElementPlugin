package hs.elementSmpUtility.listeners;

import hs.elementSmpUtility.blocks.CustomBlockManager;
import hs.elementSmpUtility.blocks.custom.PedestalBlock;
import hs.elementSmpUtility.storage.BlockDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalDataStorage;
import hs.elementSmpUtility.storage.pedestal.PedestalOwnerStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Handles player interactions with pedestal blocks
 */
public class PedestalInteractionListener implements Listener {

    private final CustomBlockManager blockManager;
    private final BlockDataStorage blockStorage;
    private final PedestalDataStorage pedestalStorage;
    private final PedestalOwnerStorage ownerStorage;

    public PedestalInteractionListener(CustomBlockManager blockManager,
                                       BlockDataStorage blockStorage,
                                       PedestalDataStorage pedestalStorage,
                                       PedestalOwnerStorage ownerStorage) {
        this.blockManager = blockManager;
        this.blockStorage = blockStorage;
        this.pedestalStorage = pedestalStorage;
        this.ownerStorage = ownerStorage;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPedestalInteract(PlayerInteractEvent event) {
        // Handle both right-click and left-click
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        // Check if it's a pedestal block
        String blockId = blockStorage.getCustomBlockIdCached(block.getLocation());
        if (blockId == null || !"pedestal".equals(blockId)) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack heldItem = player.getInventory().getItemInMainHand();

        // Get the owner of this pedestal
        UUID ownerUUID = ownerStorage.getOwner(block.getLocation());

        // Check ownership - this is CRITICAL for security
        if (ownerUUID == null) {
            player.sendActionBar(
                    Component.text("Error: This pedestal has no owner!")
                            .color(TextColor.color(0xFF5555)));
            return;
        }

        boolean isOwner = ownerUUID.equals(player.getUniqueId());
        boolean hasBypass = player.hasPermission("elementsmp.pedestal.bypass");

        // If not owner AND doesn't have bypass permission, deny ALL access
        if (!isOwner && !hasBypass) {
            event.setCancelled(true);
            String ownerName = ownerStorage.getOwnerName(block.getLocation());
            player.sendActionBar(
                    Component.text("This pedestal belongs to " + ownerName + "!")
                            .color(TextColor.color(0xFF5555))
            );
            return;
        }

        // Player is authorized (owner or has bypass)
        ItemStack currentItem = pedestalStorage.getPedestalItem(block.getLocation());

        // Auto-restore display if missing (failsafe)
        if (currentItem != null) {
            ArmorStand existing = PedestalBlock.getExistingDisplay(block.getLocation());
            if (existing == null || !existing.isValid()) {
                PedestalBlock.createOrUpdateDisplay(block.getLocation(), currentItem);
            }
        }

        // Handle rotation with SNEAK + LEFT CLICK
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && player.isSneaking()) {
            event.setCancelled(true);
            handleRotateItem(player, block, currentItem);
            return;
        }

        // Handle removal with SNEAK + RIGHT CLICK
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && player.isSneaking()) {
            event.setCancelled(true);
            handleRemoveItem(player, block, currentItem);
            return;
        }

        // Handle placement with RIGHT CLICK (no sneak)
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !player.isSneaking()) {
            event.setCancelled(true);
            handlePlaceItem(player, block, heldItem, currentItem);
            return;
        }

        // LEFT CLICK without sneak - allow breaking if owner
        // DON'T cancel the event - let it break naturally
        // The BlockBreakListener will handle the ownership check
    }

    /**
     * Handle rotating an item on the pedestal
     */
    private void handleRotateItem(Player player, Block block, ItemStack currentItem) {
        if (currentItem == null || currentItem.getType() == Material.AIR) {
            player.sendActionBar(Component.text("Pedestal is empty - nothing to rotate!")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        // Rotate the display 90 degrees
        boolean success = PedestalBlock.rotateDisplay(block.getLocation());

        if (success) {
            player.playSound(block.getLocation(), Sound.BLOCK_WOOD_STEP, 1.0f, 1.5f);
            player.sendActionBar(Component.text("Rotated item 90°")
                    .color(TextColor.color(0x55FF55)));
        } else {
            player.sendActionBar(Component.text("Failed to rotate item")
                    .color(TextColor.color(0xFF5555)));
        }
    }

    /**
     * Check if an item has special behavior that should be blocked when interacting with pedestals
     * (but the item should still be consumed when placed)
     */
    private boolean hasSpecialBehavior(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }

        // Check for Upgraders
        NamespacedKey upgraderKey = new NamespacedKey(blockManager.getPlugin(), "upgrader_level");
        if (item.getItemMeta().getPersistentDataContainer().has(upgraderKey, PersistentDataType.INTEGER)) {
            return true;
        }

        // Check for Reroller
        NamespacedKey rerollerKey = new NamespacedKey(blockManager.getPlugin(), "element_reroller");
        if (item.getItemMeta().getPersistentDataContainer().has(rerollerKey, PersistentDataType.BYTE)) {
            return true;
        }

        // Check for Advanced Reroller
        NamespacedKey advancedRerollerKey = new NamespacedKey(blockManager.getPlugin(), "advanced_reroller");
        if (item.getItemMeta().getPersistentDataContainer().has(advancedRerollerKey, PersistentDataType.BYTE)) {
            return true;
        }

        return false;
    }

    /**
     * Handle removing an item from the pedestal
     */
    private void handleRemoveItem(Player player, Block block, ItemStack currentItem) {
        if (currentItem != null) {
            // Give item back to player
            player.getInventory().addItem(currentItem.clone());

            // Remove from pedestal
            pedestalStorage.savePedestalItem(block.getLocation(), null);
            PedestalBlock.removeDisplay(block.getLocation());

            player.playSound(block.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
            player.sendActionBar(Component.text("Removed item from pedestal")
                    .color(TextColor.color(0x55FF55)));
        } else {
            player.sendActionBar(Component.text("Pedestal is empty")
                    .color(TextColor.color(0xFF5555)));
        }
    }

    /**
     * Handle placing an item on the pedestal
     */
    private void handlePlaceItem(Player player, Block block, ItemStack heldItem, ItemStack currentItem) {
        if (heldItem.getType() == Material.AIR) {
            if (currentItem != null) {
                player.sendActionBar(Component.text("Sneak + Right Click to remove | Sneak + Left Click to rotate")
                        .color(TextColor.color(0xFFAA00)));
            } else {
                player.sendActionBar(Component.text("Hold an item to place it on the pedestal")
                        .color(TextColor.color(0xFFAA00)));
            }
            return;
        }

        if (currentItem != null) {
            player.sendActionBar(Component.text("Pedestal already has an item! Sneak to remove it first.")
                    .color(TextColor.color(0xFF5555)));
            return;
        }

        // Create item to place (always just 1 item)
        ItemStack toPlace = heldItem.clone();
        toPlace.setAmount(1);

        // Save to storage and create display FIRST (before consuming item)
        pedestalStorage.savePedestalItem(block.getLocation(), toPlace);
        PedestalBlock.createOrUpdateDisplay(block.getLocation(), toPlace);

        // THEN consume one item from the player's hand
        // This ensures the item is placed even if something goes wrong
        if (heldItem.getAmount() > 1) {
            heldItem.setAmount(heldItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        player.playSound(block.getLocation(), Sound.BLOCK_STONE_PLACE, 1.0f, 1.5f);
        player.sendActionBar(Component.text("Placed item on pedestal")
                .color(TextColor.color(0x55FF55)));
    }

    /**
     * Prevent players from damaging pedestal armor stands
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onArmorStandDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof ArmorStand stand)) {
            return;
        }

        if (stand.hasMetadata("pedestal_display")) {
            event.setCancelled(true);
        }
    }
}