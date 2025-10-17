package hs.elementPlugin.elements.impl.earth.listeners;

import hs.elementPlugin.elements.upsides.impl.EarthUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles Earth element double ore drops.
 * If the player has Silk Touch, normal drops occur (no doubling).
 * Without Silk Touch, ore drops are doubled.
 */
public class EarthOreDoubleDropListener implements Listener {

    private final ElementManager elementManager;
    private final EarthUpsides earthUpsides;

    public EarthOreDoubleDropListener(ElementManager elementManager, EarthUpsides earthUpsides) {
        this.elementManager = elementManager;
        this.earthUpsides = earthUpsides;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material type = block.getType();

        // Check if player has the Earth element
        if (!earthUpsides.hasElement(player)) return;

        // Get the tool being used
        ItemStack tool = player.getInventory().getItemInMainHand();

        // If player uses Silk Touch, let vanilla handle it (no doubling)
        if (tool.getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
            return;
        }

        // Check if this ore should be doubled (checks upgrade level internally)
        if (!earthUpsides.shouldDoubleOreDrops(player, type)) return;

        // Cancel vanilla drop logic
        event.setDropItems(false);

        // Drop double the amount of items
        for (ItemStack drop : block.getDrops(tool, player)) {
            ItemStack doubled = drop.clone();
            doubled.setAmount(drop.getAmount() * 2);
            block.getWorld().dropItemNaturally(block.getLocation(), doubled);
        }
    }
}