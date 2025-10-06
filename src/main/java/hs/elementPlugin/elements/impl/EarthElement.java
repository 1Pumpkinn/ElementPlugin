package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();

        if (player.hasMetadata(META_TUNNELING)) {
            player.removeMetadata(META_TUNNELING, plugin);
            player.sendMessage(ChatColor.YELLOW + "Tunneling cancelled");
            return true;
        }

        player.setMetadata(META_TUNNELING, new FixedMetadataValue(plugin, System.currentTimeMillis() + 20_000L));
        player.sendMessage(ChatColor.GREEN + "Earth tunneling active! Look where you want to go. Sneak+Left-Click again to cancel.");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !player.hasMetadata(META_TUNNELING)) {
                    cancel();
                    return;
                }

                long until = player.getMetadata(META_TUNNELING).get(0).asLong();
                if (System.currentTimeMillis() > until) {
                    player.removeMetadata(META_TUNNELING, plugin);
                    player.sendMessage(ChatColor.YELLOW + "Tunneling ended");
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
                
                player.getWorld().spawnParticle(Particle.BLOCK, mineLocation, 10, 0.5, 0.5, 0.5, 0.1, Material.STONE.createBlockData());
            }
        }.runTaskTimer(plugin, 0L, 2L);

        return true;
    }

    private void breakTunnel(Location center, Player player) {
        Vector dir = player.getLocation().getDirection().normalize();
        Vector perpX, perpY;

        // Calculate perpendicular vectors based on look direction
        double yComponent = Math.abs(dir.getY());

        if (yComponent > 0.9) {
            perpX = new Vector(1, 0, 0);
            perpY = new Vector(0, 0, 1);
        } else if (yComponent > 0.5) {
            perpX = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
            perpY = new Vector(0, 1, 0);
        } else {
            perpX = new Vector(-dir.getZ(), 0, dir.getX()).normalize();
            perpY = new Vector(0, 1, 0);
        }

        // Create 3x3 tunnel perpendicular to look direction
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                Location loc = center.clone()
                        .add(perpX.clone().multiply(x))
                        .add(perpY.clone().multiply(y));
                Block b = loc.getBlock();

                if (TUNNELABLE.contains(b.getType())) {
                    ItemStack tool = player.getInventory().getItemInMainHand();
                    if (tool == null || tool.getType() == Material.AIR) {
                        tool = new ItemStack(Material.DIAMOND_PICKAXE);
                    }
                    
                    java.util.Collection<ItemStack> drops = b.getDrops(tool);
                    b.setType(Material.AIR);
                    
                    for (ItemStack drop : drops) {
                        if (drop != null && drop.getType() != Material.AIR) {
                            b.getWorld().dropItemNaturally(loc, drop);
                        }
                    }
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


