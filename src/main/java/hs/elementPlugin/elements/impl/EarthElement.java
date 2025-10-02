package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.Set;

public class EarthElement extends BaseElement {
    public static final String META_MINE_UNTIL = "earth_mine_until";
    public static final String META_CHARM_NEXT_UNTIL = "earth_charm_next_until";
    public static final String META_TUNNELING = "earth_tunneling";

    private static final Set<Material> TUNNELABLE = EnumSet.of(
            Material.STONE, Material.DEEPSLATE, Material.DIRT, Material.GRASS_BLOCK,
            Material.COBBLESTONE, Material.ANDESITE, Material.DIORITE, Material.GRANITE,
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.GRAVEL, Material.SAND, Material.RED_SAND, Material.SANDSTONE,
            Material.TUFF, Material.CALCITE, Material.DRIPSTONE_BLOCK
    );

    public EarthElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.EARTH; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Hero of the Village I
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();

        if (player.hasMetadata(META_TUNNELING)) {
            // Cancel tunneling
            player.removeMetadata(META_TUNNELING, plugin);
            player.setSwimming(false);
            player.setGravity(true);
            player.sendMessage(ChatColor.YELLOW + "Tunneling cancelled");
            return true;
        }

        // Start tunneling mode
        player.setMetadata(META_TUNNELING, new FixedMetadataValue(plugin, System.currentTimeMillis() + 20_000L));
        player.sendMessage(ChatColor.GREEN + "Earth tunneling active! Look where you want to go. Sneak+Left-Click again to cancel.");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata(META_TUNNELING)) {
                    player.setSwimming(false);
                    player.setGravity(true);
                    cancel();
                    return;
                }

                long until = player.getMetadata(META_TUNNELING).get(0).asLong();
                if (System.currentTimeMillis() > until) {
                    player.removeMetadata(META_TUNNELING, plugin);
                    player.setSwimming(false);
                    player.setGravity(true);
                    player.sendMessage(ChatColor.YELLOW + "Tunneling ended");
                    cancel();
                    return;
                }

                // Make player swim and move forward
                player.setSwimming(true);
                player.setGravity(false);

                Vector direction = player.getLocation().getDirection().normalize().multiply(0.3);
                player.setVelocity(direction);

                // Break blocks in a 3x3 area in front of player
                Location front = player.getEyeLocation().add(player.getLocation().getDirection().multiply(1.5));
                breakTunnel(front, player);

                // Particles
                player.getWorld().spawnParticle(Particle.BLOCK, front, 10, 0.5, 0.5, 0.5, 0.1, Material.STONE.createBlockData());
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void breakTunnel(Location center, Player player) {
        Vector dir = player.getLocation().getDirection().normalize();
        Vector perpX, perpY;

        // Get perpendicular vectors for the plane based on look direction
        double yComponent = Math.abs(dir.getY());

        if (yComponent > 0.9) {
            // Looking nearly straight up or down - use X and Z as perpendiculars
            perpX = new Vector(1, 0, 0);
            perpY = new Vector(0, 0, 1);
        } else if (yComponent > 0.5) {
            // Looking at an angle - blend between horizontal and vertical planes
            perpX = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
            perpY = new Vector(0, 1, 0);
        } else {
            // Looking mostly horizontally
            perpX = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
            perpY = new Vector(0, 1, 0);
        }

        // Break 3x3 grid perpendicular to look direction
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                Location loc = center.clone()
                        .add(perpX.clone().multiply(x))
                        .add(perpY.clone().multiply(y));
                Block b = loc.getBlock();

                if (TUNNELABLE.contains(b.getType())) {
                    b.breakNaturally(player.getInventory().getItemInMainHand(), true);
                }
            }
        }
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        long until = System.currentTimeMillis() + 30_000L;
        player.setMetadata(META_CHARM_NEXT_UNTIL, new FixedMetadataValue(plugin, until));
        player.sendMessage(ChatColor.GOLD + "Punch a mob to charm it for 30s - it will follow you!");
        return true;
    }
}