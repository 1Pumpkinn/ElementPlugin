package hs.elementPlugin.listeners.player;

import hs.elementPlugin.elements.upsides.impl.EarthUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public class EarthOreListener implements Listener {
    private final ElementManager elementManager;
    private final EarthUpsides earthUpsides;

    public EarthOreListener(ElementManager elementManager, EarthUpsides earthUpsides) {
        this.elementManager = elementManager;
        this.earthUpsides = earthUpsides;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getBlock();
        Material blockType = block.getType();

        // Check if player should get double ore drops
        if (!earthUpsides.shouldDoubleOreDrops(player, blockType)) {
            return;
        }

        // Don't apply in creative mode
        if (player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        // Get the normal drops
        ItemStack tool = player.getInventory().getItemInMainHand();
        Collection<ItemStack> drops = block.getDrops(tool, player);

        // Cancel the event to prevent normal drops
        event.setDropItems(false);

        // Drop double the items
        for (ItemStack drop : drops) {
            ItemStack doubleDrop = drop.clone();
            doubleDrop.setAmount(drop.getAmount() * 2);
            block.getWorld().dropItemNaturally(block.getLocation(), doubleDrop);
        }
    }
}