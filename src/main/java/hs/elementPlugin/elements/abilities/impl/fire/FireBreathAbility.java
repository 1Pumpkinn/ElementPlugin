package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FireBreathAbility extends BaseAbility {
    private final Set<UUID> activeUsers = new HashSet<>();
    private final hs.elementPlugin.ElementPlugin plugin;
    
    public FireBreathAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("fire_breath", 50, 10, 1);
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        
        // Clear any existing active state first to prevent glitches
        setActive(player, false);
        
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        setActive(player, true);
        
        player.sendMessage(ChatColor.GOLD + "Fire Breath activated!");

        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 100; // 5 seconds
            private boolean isCancelled = false;

            @Override
            public void run() {
                if (isCancelled || !player.isOnline() || ticks >= maxTicks) {
                    setActive(player, false);
                    player.sendMessage(ChatColor.GOLD + "Fire Breath deactivated!");
                    cancel();
                    return;
                }

                // Create a more focused cone of fire particles
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection();
                
                double maxDistance = 8.0;
                double coneAngle = Math.PI / 12; // 15 degrees (narrower cone)
                double startDistance = 1.5; // Start particles further from player's face
                
                for (double distance = startDistance; distance <= maxDistance; distance += 0.8) {
                    Location particleLoc = eyeLoc.clone().add(direction.clone().multiply(distance));
                    
                    // Check for blocks to limit distance
                    if (!particleLoc.getBlock().isPassable()) {
                        break;
                    }
                    
                    double radius = (distance - startDistance) * 0.15; // Smaller radius multiplier
                    
                    // Create fewer particles in a tighter cone
                    for (int i = 0; i < 8; i++) { // Reduced from 12 to 8 particles per circle
                        double angle = (2 * Math.PI * i) / 8;
                        Vector offset = getPerpendicular(direction).multiply(radius * Math.cos(angle))
                                .add(getPerpendicular(getPerpendicular(direction)).multiply(radius * Math.sin(angle)));
                        
                        Location finalLoc = particleLoc.clone().add(offset);
                        
                        // Spawn fewer FLAME particles with smaller spread
                        player.getWorld().spawnParticle(Particle.FLAME, finalLoc, 2, 0.1, 0.1, 0.1, 0.02);
                        
                        // Less frequent LAVA particles, only beyond startDistance + 1.0
                        if (distance > startDistance + 1.0 && Math.random() < 0.3) {
                            player.getWorld().spawnParticle(Particle.LAVA, finalLoc, 1, 0.1, 0.1, 0.1, 0);
                        }
                    }
                    
                    // Check for entities in the cone and set them on fire
                    for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                        if (entity == player) continue;
                        
                        Location entityLoc = entity.getEyeLocation();
                        Vector toEntity = entityLoc.toVector().subtract(eyeLoc.toVector());
                        double distanceToEntity = toEntity.length();
                        
                        if (distanceToEntity > maxDistance || distanceToEntity < startDistance) continue;
                        
                        toEntity.normalize();
                        double dot = toEntity.dot(direction);
                        double angle = Math.acos(dot);
                        
                        if (angle <= coneAngle) {
                            entity.setFireTicks(Math.max(entity.getFireTicks(), 60)); // 3 seconds of fire
                            
                            // Apply damage every 10 ticks (0.5 seconds)
                            if (ticks % 10 == 0) {
                                entity.damage(2.0, player); // 1 heart of damage
                            }
                        }
                    }
                }
                
                ticks++;
            }
            
            @Override
            public void cancel() {
                super.cancel();
                isCancelled = true;
            }
        }.runTaskTimer(context.getPlugin(), 0L, 1L);
        
        return true;
    }
    
    // Using the base class implementation for isActiveFor and setActive
    
    public void clearEffects(Player player) {
        setActive(player, false);
    }
    
    @Override
    public String getName() {
        return ChatColor.RED + "Fire Breath";
    }
    
    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Breathe a cone of fire that ignites enemies. (50 mana)";
    }
    
    // Helper method to get a perpendicular vector
    private Vector getPerpendicular(Vector vector) {
        if (vector.getX() == 0 && vector.getZ() == 0) {
            return new Vector(1, 0, 0);
        } else {
            return new Vector(-vector.getZ(), 0, vector.getX()).normalize();
        }
    }
}