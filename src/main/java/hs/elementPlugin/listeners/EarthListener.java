package hs.elementPlugin.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.impl.EarthElement;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class EarthListener implements Listener {
    private final ElementManager elements;

    public EarthListener(ElementManager elements) {
        this.elements = elements;
    }

    private static final Set<Material> STONE_TYPES = EnumSet.of(Material.STONE, Material.DEEPSLATE);

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!p.hasMetadata(EarthElement.META_MINE_UNTIL)) return;
        long until = p.getMetadata(EarthElement.META_MINE_UNTIL).get(0).asLong();
        if (System.currentTimeMillis() > until) return;
        Block base = e.getBlock();
        if (!STONE_TYPES.contains(base.getType())) return;

        // Break a 3x3 plane perpendicular to facing
        org.bukkit.block.BlockFace face = p.getFacing().getOppositeFace();
        // Decide plane axes
        int[][] offsets;
        switch (p.getFacing()) {
            case NORTH, SOUTH -> offsets = planeOffsets('x', 'y');
            case EAST, WEST -> offsets = planeOffsets('z', 'y');
            case UP, DOWN -> offsets = planeOffsets('x', 'z');
            default -> offsets = planeOffsets('x', 'y');
        }
        ItemStack tool = p.getInventory().getItemInMainHand();
        for (int[] off : offsets) {
            Block b = base.getRelative(off[0], off[1], off[2]);
            if (STONE_TYPES.contains(b.getType())) {
                b.breakNaturally(tool, true);
            }
        }
    }

    private int[][] planeOffsets(char a1, char a2) {
        int[][] arr = new int[9][3];
        int i = 0;
        for (int u = -1; u <= 1; u++) {
            for (int v = -1; v <= 1; v++) {
                int x=0,y=0,z=0;
                if (a1=='x') x=u; if (a1=='y') y=u; if (a1=='z') z=u;
                if (a2=='x') x=v; if (a2=='y') y=v; if (a2=='z') z=v;
                arr[i++]=new int[]{x,y,z};
            }
        }
        return arr;
    }

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
    );

    @EventHandler
    public void onDrop(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        var pd = elements.data(p.getUniqueId());
        if (pd.getCurrentElement() != ElementType.EARTH || pd.getUpgradeLevel(ElementType.EARTH) < 2) return;
        if (!ORES.contains(e.getBlockState().getType())) return;
        e.getItems().forEach(item -> item.getItemStack().setAmount(item.getItemStack().getAmount() * 2));
    }

    @EventHandler
    public void onPunch(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof LivingEntity mob)) return;
        if (!p.hasMetadata(EarthElement.META_CHARM_NEXT_UNTIL)) return;
        long until = p.getMetadata(EarthElement.META_CHARM_NEXT_UNTIL).get(0).asLong();
        if (System.currentTimeMillis() > until) return;
        // consume
        p.removeMetadata(EarthElement.META_CHARM_NEXT_UNTIL, hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class));
        long expire = System.currentTimeMillis() + 30_000L;
        mob.setMetadata("earth_charmed_owner", new org.bukkit.metadata.FixedMetadataValue(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), p.getUniqueId().toString()));
        mob.setMetadata("earth_charmed_until", new org.bukkit.metadata.FixedMetadataValue(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), expire));
        p.sendMessage(org.bukkit.ChatColor.GREEN + "Charmed for 30s");
    }
}