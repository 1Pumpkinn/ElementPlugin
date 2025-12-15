package saturn.elementPlugin.elements.impl.earth.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

/**
 * Earth Upside 2: 1.5x ore drops (requires Upgrade II)
 * This applies to ALL ore types when broken
 */
public class EarthOreDropListener implements Listener {
    private final ElementManager elements;

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
            Material.ANCIENT_DEBRIS
    );

    public EarthOreDropListener(ElementManager elements) {
        this.elements = elements;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        var pd = elements.data(p.getUniqueId());

        // Check element and upgrade level
        if (pd.getCurrentElement() != ElementType.EARTH || pd.getUpgradeLevel(ElementType.EARTH) < 2) {
            return;
        }

        // Check if it's an ore
        if (!ORES.contains(e.getBlockState().getType())) {
            return;
        }

        // Don't multiply drops if using Silk Touch
        ItemStack tool = p.getInventory().getItemInMainHand();
        if (tool.containsEnchantment(Enchantment.SILK_TOUCH)) {
            return;
        }

        // FIXED: Apply 1.5x multiplier (50% increase) to ALL drops
        e.getItems().forEach(item -> {
            int originalAmount = item.getItemStack().getAmount();

            // Calculate 1.5x: amount + (amount / 2)
            // For odd numbers, round up to ensure at least 1 extra item
            int bonus = (originalAmount + 1) / 2; // This rounds up for odd numbers
            int newAmount = originalAmount + bonus;

            item.getItemStack().setAmount(newAmount);
        });
    }
}