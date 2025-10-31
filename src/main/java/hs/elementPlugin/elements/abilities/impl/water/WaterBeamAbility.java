package hs.elementPlugin.elements.abilities.impl.water;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class WaterBeamAbility extends BaseAbility {
    private final Set<UUID> activeUsers = new HashSet<>();
    private final hs.elementPlugin.ElementPlugin plugin;

    public WaterBeamAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("water_beam", 50, 15, 2);
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.2f);

        setActive(player, true);

        new BukkitRunnable() {
            int ticks = 0;
            double totalDamageDealt = 0;
            @Override
            public void run() {
                // Ability ends after 10 seconds or 5 hearts of damage
                if (!player.isOnline() || ticks >= 200 || totalDamageDealt >= 10) {
                    setActive(player, false);
                    cancel();
                    return;
                }
                Vector dir = player.getLocation().getDirection().normalize();

                // Apply damage every 0.25 seconds
                if (ticks % 5 == 0) {
                    Location chestLoc = player.getLocation().add(0, 1.2, 0);
                    
                    // Check for blocks in the way first, but allow passage through small blocks
                    double maxDistance = 20.0;
                    for (double d = 0; d <= 20.0; d += 0.5) {
                        Location checkLoc = chestLoc.clone().add(dir.clone().multiply(d));
                        Block block = checkLoc.getBlock();
                        if (!isPassableBlock(block)) {
                            maxDistance = d;
                            break;
                        }
                    }
                    
                    // Now trace entities but only up to the nearest block
                    RayTraceResult r = player.getWorld().rayTraceEntities(chestLoc, dir, maxDistance,
                            entity -> entity instanceof LivingEntity && !entity.equals(player));
                    if (r != null && r.getHitEntity() instanceof LivingEntity le) {
                        if (isValidTarget(context, le)) {
                            // Apply knockback with slight upward component
                            Vector knockback = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                            knockback.setY(0.2);
                            knockback = knockback.multiply(0.8);
                            le.setVelocity(knockback);

                            if (totalDamageDealt < 10) {
                                double damageAmount = 0.5;
                                if (totalDamageDealt + damageAmount > 10) {
                                    damageAmount = 10 - totalDamageDealt;
                                }
                                // Instead of just subtracting health, use true Bukkit damage
                                le.damage(damageAmount, player);
                                totalDamageDealt += damageAmount;

                                Location hit = r.getHitPosition().toLocation(player.getWorld());

                                try {
									if (le instanceof Player) {
										player.getWorld().spawnParticle(Particle.SPLASH, hit, 15, 0.3, 0.3, 0.3, 0.2, null, true);
										player.getWorld().spawnParticle(Particle.BUBBLE_POP, hit, 10, 0.2, 0.2, 0.2, 0.1, null, true);
                                        player.getWorld().playSound(hit, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.5f);

                                        // Create circular water ring effect
                                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                                            double x = Math.cos(angle) * 0.5;
                                            double z = Math.sin(angle) * 0.5;
											Location ringLoc = hit.clone().add(x, 0.1, z);
											player.getWorld().spawnParticle(Particle.BUBBLE, ringLoc, 1, 0.05, 0.05, 0.05, 0.0, null, true);
                                        }
									} else {
										player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, hit, 3, 0.1, 0.1, 0.1, 0.0, null, true);
                                    }
                                } catch (Exception e) {
                                    // Ignore particle errors
                                }
                            }
                        }
                    }
                }

                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection();
                
                double maxBeamDistance = 20.0;
                double particleDistance = 0.5;
                
                for (double d = 0; d <= maxBeamDistance; d += particleDistance) {
                    Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(d));
                    
                    if (!isPassableBlock(particleLoc.getBlock())) {
                        break;
                    }
                    
						if (ticks % 2 == 0) {
							player.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 1, 0.05, 0.05, 0.05, 0.01, null, true);
							
							if (d % 2 < 0.5) {
								player.getWorld().spawnParticle(Particle.BUBBLE_POP, particleLoc, 1, 0.05, 0.05, 0.05, 0.01, null, true);
							}
                    }
                }
                
                ticks++;
            }
        }.runTaskTimer(context.getPlugin(), 0L, 1L);
        
        return true;
    }
    
    @Override
    public boolean isActiveFor(Player player) {
        return activeUsers.contains(player.getUniqueId());
    }
    
    @Override
    public void setActive(Player player, boolean active) {
        if (active) {
            activeUsers.add(player.getUniqueId());
        } else {
            activeUsers.remove(player.getUniqueId());
        }
    }
    
    public void clearEffects(Player player) { 
         setActive(player, false); 
     } 
     
     @Override
     public String getName() { 
         return ChatColor.AQUA + "Water Beam"; 
     } 
     
     @Override
     public String getDescription() { 
         return ChatColor.GRAY + "Fire a continuous beam of water that damages and pushes back enemies. (40 mana)"; 
     }
    
    @Override
    protected boolean isValidTarget(ElementContext context, LivingEntity entity) {
        return true;
    }
    
    private boolean isPassableBlock(Block block) {
        if (block == null) return true;
        
        Material type = block.getType();
        
        if (type == Material.AIR || type == Material.VOID_AIR || type == Material.CAVE_AIR) {
            return true;
        }
        
        return type == Material.SHORT_GRASS ||
               type == Material.TALL_GRASS ||
                type == Material.SHORT_DRY_GRASS ||
                type == Material.TALL_DRY_GRASS ||
                type == Material.FIREFLY_BUSH ||
                type == Material.FERN ||
               type == Material.LARGE_FERN ||
               type == Material.DEAD_BUSH ||
               type == Material.DANDELION ||
               type == Material.POPPY ||
               type == Material.BLUE_ORCHID ||
               type == Material.ALLIUM ||
               type == Material.AZURE_BLUET ||
               type == Material.RED_TULIP ||
               type == Material.ORANGE_TULIP ||
               type == Material.WHITE_TULIP ||
               type == Material.PINK_TULIP ||
               type == Material.OXEYE_DAISY ||
               type == Material.CORNFLOWER ||
               type == Material.LILY_OF_THE_VALLEY ||
               type == Material.SUNFLOWER ||
               type == Material.LILAC ||
               type == Material.ROSE_BUSH ||
               type == Material.PEONY ||
               type == Material.SWEET_BERRY_BUSH ||
               type == Material.BAMBOO ||
               type == Material.SUGAR_CANE ||
               type == Material.KELP ||
               type == Material.SEAGRASS ||
               type == Material.TALL_SEAGRASS ||
               type == Material.WHEAT ||
               type == Material.CARROTS ||
               type == Material.POTATOES ||
               type == Material.BEETROOTS ||
               type == Material.MELON_STEM ||
               type == Material.PUMPKIN_STEM ||
               type == Material.TORCH ||
               type == Material.REDSTONE_TORCH ||
               type == Material.SOUL_TORCH ||
               type == Material.REDSTONE_WIRE ||
               type == Material.TRIPWIRE ||
               type == Material.TRIPWIRE_HOOK ||
               type == Material.LEVER ||
               type == Material.STONE_BUTTON ||
               type == Material.OAK_BUTTON ||
               type == Material.SPRUCE_BUTTON ||
               type == Material.BIRCH_BUTTON ||
               type == Material.JUNGLE_BUTTON ||
               type == Material.ACACIA_BUTTON ||
               type == Material.DARK_OAK_BUTTON ||
               type == Material.CRIMSON_BUTTON ||
               type == Material.WARPED_BUTTON ||
               type == Material.POLISHED_BLACKSTONE_BUTTON ||
               type == Material.LIGHT_WEIGHTED_PRESSURE_PLATE ||
               type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE ||
               type == Material.SPRUCE_PRESSURE_PLATE ||
               type == Material.BIRCH_PRESSURE_PLATE ||
               type == Material.JUNGLE_PRESSURE_PLATE ||
               type == Material.ACACIA_PRESSURE_PLATE ||
               type == Material.DARK_OAK_PRESSURE_PLATE ||
               type == Material.CRIMSON_PRESSURE_PLATE ||
               type == Material.WARPED_PRESSURE_PLATE ||
               type == Material.POLISHED_BLACKSTONE_PRESSURE_PLATE ||
               type == Material.STONE_PRESSURE_PLATE ||
               type == Material.RAIL ||
               type == Material.POWERED_RAIL ||
               type == Material.DETECTOR_RAIL ||
               type == Material.ACTIVATOR_RAIL ||
               type == Material.COBWEB ||
               type == Material.VINE ||
               type == Material.LADDER ||
               type == Material.SCAFFOLDING ||
               type == Material.SNOW ||
               type == Material.WATER ||
               type == Material.LAVA ||
               type == Material.FIRE ||
               type == Material.SOUL_FIRE;
    }
}