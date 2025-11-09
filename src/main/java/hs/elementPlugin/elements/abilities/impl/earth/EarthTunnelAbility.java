package hs.elementPlugin.elements.abilities.impl.earth;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.impl.earth.EarthElement;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.Set;

public class EarthTunnelAbility extends BaseAbility {
    private static final Set<Material> TUNNELABLE = EnumSet.of(
            Material.STONE, Material.DEEPSLATE, Material.DIRT, Material.GRASS_BLOCK,
            Material.COBBLESTONE, Material.ANDESITE, Material.DIORITE, Material.GRANITE,

            Material.GRAVEL, Material.SAND, Material.RED_SAND, Material.SANDSTONE,
            Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK,

            // Nether Blocks
            Material.BLACKSTONE, Material.ANCIENT_DEBRIS, Material.CRIMSON_NYLIUM,
            Material.WARPED_HYPHAE, Material.SOUL_SAND, Material.BASALT, Material.SOUL_SOIL,
            Material.NETHERRACK,

            // Ores

            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,

            Material.NETHERITE_BLOCK,
            Material.DIAMOND_BLOCK,
            Material.EMERALD_BLOCK,
            Material.GOLD_BLOCK,
            Material.REDSTONE_BLOCK,
            Material.LAPIS_BLOCK,
            Material.IRON_BLOCK,
            Material.COPPER_BLOCK,
            Material.COAL_BLOCK
    );

    private final hs.elementPlugin.ElementPlugin plugin;

    public EarthTunnelAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("earth_tunnel", 50, 10, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // If ability is already active (check metadata), cancel it WITHOUT consuming mana
        if (player.hasMetadata(EarthElement.META_TUNNELING)) {
            player.removeMetadata(EarthElement.META_TUNNELING, plugin);
            player.sendMessage(ChatColor.YELLOW + "Tunneling cancelled");
            setActive(player, false);
            // Don't start a new tunnel - just cancel and return
            return true;
        }

        // Start the tunneling ability
        player.setMetadata(EarthElement.META_TUNNELING, new FixedMetadataValue(plugin, System.currentTimeMillis() + 20_000L));
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);
        player.sendMessage(ChatColor.GOLD + "Tunneling started Press again to cancel.");

        setActive(player, true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata(EarthElement.META_TUNNELING)) {
                    setActive(player, false);
                    cancel();
                    return;
                }

                long until = player.getMetadata(EarthElement.META_TUNNELING).get(0).asLong();
                if (System.currentTimeMillis() > until) {
                    player.removeMetadata(EarthElement.META_TUNNELING, plugin);
                    player.sendMessage(ChatColor.YELLOW + "Tunneling ended");
                    setActive(player, false);
                    cancel();
                    return;
                }

                Vector direction = player.getLocation().getDirection().normalize();

                // Adjust mine location based on look direction
                Location mineLocation;
                if (direction.getY() < -0.5) {
                    mineLocation = player.getLocation().add(direction.multiply(1.0));
                } else {
                    mineLocation = player.getEyeLocation().add(direction.multiply(1.5));
                }
                breakTunnel(mineLocation, player);

                player.getWorld().spawnParticle(Particle.BLOCK, mineLocation, 10, 0.5, 0.5, 0.5, 0.1, Material.STONE.createBlockData(), true);
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void breakTunnel(Location center, Player player) {
        World world = center.getWorld();
        if (world == null) return;

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    Location blockLoc = center.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();

                    if (TUNNELABLE.contains(block.getType())) {
                        block.breakNaturally();
                        world.playSound(blockLoc, Sound.BLOCK_STONE_BREAK, 0.3f, 1.0f);
                    }
                }
            }
        }
    }

    @Override
    public String getName() {
        return ChatColor.GOLD + "Earth Tunnel";
    }

    @Override
    public String getDescription() {
        return "Create a tunnel through earth and stone by looking in the direction you want to dig. Press again to cancel.";
    }
}