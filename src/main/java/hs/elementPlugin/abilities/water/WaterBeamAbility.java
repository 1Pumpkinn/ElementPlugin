package hs.elementPlugin.abilities.water;

import hs.elementPlugin.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
        super("water_beam", 40, 15, 1);
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        player.sendMessage(ChatColor.AQUA + "Water beam active for 10s...");
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
                    
                    // Check for blocks in the way first
                    RayTraceResult blockResult = player.getWorld().rayTraceBlocks(chestLoc, dir, 20.0);
                    double maxDistance = 20.0;
                    if (blockResult != null && blockResult.getHitBlock() != null) {
                        maxDistance = blockResult.getHitPosition().distance(chestLoc.toVector());
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

                                double currentHealth = le.getHealth();
                                double newHealth = Math.max(0, currentHealth - damageAmount);
                                le.setHealth(newHealth);
                                totalDamageDealt += damageAmount;

                                Location hit = r.getHitPosition().toLocation(player.getWorld());

                                try {
                                    if (le instanceof Player) {
                                        player.getWorld().spawnParticle(Particle.SPLASH, hit, 15, 0.3, 0.3, 0.3, 0.2);
                                        player.getWorld().spawnParticle(Particle.BUBBLE_POP, hit, 10, 0.2, 0.2, 0.2, 0.1);
                                        player.getWorld().playSound(hit, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.5f);

                                        // Create circular water ring effect
                                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                                            double x = Math.cos(angle) * 0.5;
                                            double z = Math.sin(angle) * 0.5;
                                            Location ringLoc = hit.clone().add(x, 0.1, z);
                                            player.getWorld().spawnParticle(Particle.BUBBLE, ringLoc, 1, 0.05, 0.05, 0.05, 0.0);
                                        }
                                    } else {
                                        player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, hit, 3, 0.1, 0.1, 0.1, 0.0);
                                    }
                                } catch (Exception e) {
                                    // Ignore particle errors
                                }
                            }
                        }
                    }
                }

                // Visualize the beam
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection();
                
                double maxBeamDistance = 20.0;
                double particleDistance = 0.5;
                
                for (double d = 0; d <= maxBeamDistance; d += particleDistance) {
                    Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(d));
                    
                    // Stop if we hit a block
                    if (!particleLoc.getBlock().isPassable()) {
                        break;
                    }
                    
                    // Spawn water particles along the beam
                    if (ticks % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.SPLASH, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                        
                        // Add some bubble particles occasionally
                        if (d % 2 < 0.5) {
                            player.getWorld().spawnParticle(Particle.BUBBLE_POP, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
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
    
    // Helper method to check if an entity is a valid target
    @Override
    protected boolean isValidTarget(ElementContext context, LivingEntity entity) {
        // Add your target validation logic here
        return true;
    }
}