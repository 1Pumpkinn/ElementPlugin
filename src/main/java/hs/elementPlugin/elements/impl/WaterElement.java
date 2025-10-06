package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class WaterElement extends BaseElement {

    public WaterElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.WATER; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Infinite Conduit Power I
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
        // Upside 2: Dolphins Grace 5
        if (upgradeLevel >= 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 4, true, false));
        }
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        Location playerLoc = player.getLocation();

        // Launch all valid targets in a 5x5 radius around the player
        boolean foundTargets = false;
        for (LivingEntity entity : playerLoc.getNearbyLivingEntities(5.0)) {
            if (entity.equals(player)) continue; // Don't launch self
            if (!isValidTarget(context, entity)) continue; // Skip trusted players
            
            foundTargets = true;
            final LivingEntity target = entity;
            final double startY = target.getLocation().getY();

            // Create initial geyser effect
            Location targetLoc = target.getLocation();
            Location groundLoc = new Location(targetLoc.getWorld(), targetLoc.getX(), startY, targetLoc.getZ());
            
            // Initial geyser burst
            targetLoc.getWorld().playSound(groundLoc, Sound.BLOCK_WATER_AMBIENT, 1.0f, 0.5f);
            targetLoc.getWorld().spawnParticle(Particle.SPLASH, groundLoc, 30, 0.5, 0.1, 0.5, 0.3);
            
            // Launch each target individually with enhanced geyser effect
            new BukkitRunnable() {
                int ticks = 0;
                double geyserHeight = 0;
                double lastGeyserHeight = 0;
                
                @Override
                public void run() {
                    if (target.isDead() || !target.isValid()) { cancel(); return; }
                    Location loc = target.getLocation();
                    target.setVelocity(new Vector(0, 1.2, 0));
                    
                    // Bubble column under the entity (reduced count)
                    target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc.getX(), loc.getY() - 0.01, loc.getZ(), 5, 0.2, 0.0, 0.2, 0.01);
                    
                    // Create shooting geyser effect from ground to current height
                    Location groundLoc = new Location(loc.getWorld(), loc.getX(), startY, loc.getZ());
                    
                    // Calculate current geyser height (increases with each tick)
                    // Use a smoother transition by limiting how much the height can change per tick
                    double targetHeight = Math.min(loc.getY() - startY, 20);
                    double heightDiff = targetHeight - lastGeyserHeight;
                    // Smooth transition - only grow by a small amount each tick
                    geyserHeight = lastGeyserHeight + Math.min(heightDiff, 0.5);
                    lastGeyserHeight = geyserHeight;
                    
                    // Create particles along the entire geyser column with smaller increments for smoother appearance
                    for (double y = 0; y <= geyserHeight; y += 0.25) {
                        // Skip some iterations to reduce particle count but maintain smoothness
                        if (y % 0.5 != 0 && y > 1) continue;
                        
                        Location particleLoc = groundLoc.clone().add(0, y, 0);
                        double spread = 0.15 * (1 - (y / Math.max(geyserHeight, 1))); // Wider at bottom, narrower at top
                        
                        // Main geyser column - fewer particles per point but more points
                        target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, particleLoc, 1, spread, 0.05, spread, 0.01);
                        
                        // Add water splash particles less frequently
                        if (y % 1.5 < 0.25 && y > 0.5) {
                            target.getWorld().spawnParticle(Particle.UNDERWATER, particleLoc, 1, spread, 0.05, spread, 0.01);
                        }
                    }
                    
                    // Add splash effect at the base (reduced and more consistent)
                    if (ticks % 3 == 0) {
                        target.getWorld().spawnParticle(Particle.SPLASH, groundLoc, 3, 0.2, 0.05, 0.2, 0.05);
                    }
                    
                    ticks++;
                    if (loc.getY() - startY >= 20 || ticks >= 25) {
                        // Final burst of particles when the effect ends
                        target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, groundLoc, 25, 0.5, 0.2, 0.5, 0.1);
                        target.getWorld().spawnParticle(Particle.SPLASH, loc, 20, 0.5, 0.2, 0.5, 0.2);
                        target.getWorld().playSound(groundLoc, Sound.BLOCK_WATER_AMBIENT, 1.0f, 1.0f);
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, 2L);
        }

        if (!foundTargets) {
            player.sendMessage(ChatColor.YELLOW + "No valid targets within 5 blocks.");
            return false;
        }

        player.getWorld().playSound(playerLoc, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1f);
        player.sendMessage(ChatColor.AQUA + "Water geyser launched nearby enemies!");
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();

        player.sendMessage(ChatColor.AQUA + "Water beam active for 10s...");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.2f);

        // Mark ability as active to prevent stacking
        setAbility2Active(player, true);

        // Store the task ID so we can properly cancel it if player dies or logs off
        final int[] taskId = {-1};

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            double totalDamageDealt = 0; // Track total damage dealt
            @Override
            public void run() {
                // Make sure to properly clean up if player is offline or ability ends
                if (!player.isOnline() || ticks >= 200 || totalDamageDealt >= 10) { // Run for 10 seconds (200 ticks) or until 5 hearts (10 damage) is dealt
                    setAbility2Active(player, false);
                    cancel();
                    return;
                }
                Vector dir = player.getLocation().getDirection().normalize();

                // Damage every 5 ticks (0.25 seconds) for constant damage
                if (ticks % 5 == 0) {
                    Location chestLoc = player.getLocation().add(0, 1.2, 0);
                    RayTraceResult r = player.getWorld().rayTraceEntities(chestLoc, dir, 20.0,
                            entity -> entity instanceof LivingEntity && !entity.equals(player));
                    if (r != null && r.getHitEntity() instanceof LivingEntity le) {
                        if (isValidTarget(context, le)) {
                            // Apply knockback effect
                            Vector knockback = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                            knockback.setY(0.2); // Add slight upward component
                            knockback = knockback.multiply(0.8); // Moderate knockback strength
                            le.setVelocity(knockback);

                            // Only deal damage if we haven't reached 5 hearts total
                            if (totalDamageDealt < 10) {
                                // True damage that bypasses armor - 0.5 damage per hit
                                double damageAmount = 0.5;
                                // Make sure we don't exceed 5 hearts total
                                if (totalDamageDealt + damageAmount > 10) {
                                    damageAmount = 10 - totalDamageDealt;
                                }

                                double currentHealth = le.getHealth();
                                double newHealth = Math.max(0, currentHealth - damageAmount);
                                le.setHealth(newHealth);
                                totalDamageDealt += damageAmount;

                                Location hit = r.getHitPosition().toLocation(player.getWorld());

                                try {
                                    // Enhanced hit effect based on the image
                                    if (le instanceof Player) {
                                        // Create a more dramatic effect for players
                                        // Water splash particles
                                        player.getWorld().spawnParticle(Particle.SPLASH, hit, 15, 0.3, 0.3, 0.3, 0.2);
                                        // Bubble pop particles
                                        player.getWorld().spawnParticle(Particle.BUBBLE_POP, hit, 10, 0.2, 0.2, 0.2, 0.1);
                                        // Play splash sound
                                        player.getWorld().playSound(hit, Sound.ENTITY_PLAYER_SPLASH, 0.8f, 1.5f);

                                        // Create water ring effect
                                        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                                            double x = Math.cos(angle) * 0.5;
                                            double z = Math.sin(angle) * 0.5;
                                            Location ringLoc = hit.clone().add(x, 0.1, z);
                                            player.getWorld().spawnParticle(Particle.BUBBLE, ringLoc, 1, 0.05, 0.05, 0.05, 0.0);
                                        }
                                    } else {
                                        // Original effect for non-player entities
                                        player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, hit, 3, 0.1, 0.1, 0.1, 0.0);
                                    }
                                } catch (Exception e) {
                                    // Catch any particle errors to prevent ability from breaking
                                    plugin.getLogger().warning("Error spawning water beam particles: " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                try {
                    // Draw steady vertical geyser beam - only spawn particles every few ticks to prevent flashing
                    if (ticks % 2 == 0) {
                        Location chestLoc = player.getLocation().add(0, 1.2, 0);
                        // Create a straight upward geyser effect
                        for (double d = 0; d <= 20; d += 0.5) {
                            Location pt = chestLoc.clone().add(dir.clone().multiply(d));
                            // Use BUBBLE_COLUMN_UP with no spread for a tight, straight beam
                            player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, pt, 1, 0.0, 0.0, 0.0, 0.0);
                            // Add occasional water droplets for visual interest
                            if (d % 2.0 < 0.5) {
                                player.getWorld().spawnParticle(Particle.DRIPPING_WATER, pt, 1, 0.0, 0.0, 0.0, 0.0);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Catch any particle errors to prevent ability from breaking
                    plugin.getLogger().warning("Error spawning water beam particles: " + e.getMessage());
                }

                ticks++;
            }
        };

        // Store the task ID and start the task
        taskId[0] = task.runTaskTimer(plugin, 0L, 1L).getTaskId();

        // Register a listener to ensure ability is properly deactivated if player dies or logs off
        plugin.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onPlayerQuit(PlayerQuitEvent event) {
                if (event.getPlayer().equals(player)) {
                    setAbility2Active(player, false);
                    if (taskId[0] != -1) {
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                    }
                    HandlerList.unregisterAll(this);
                }
            }

            @EventHandler
            public void onPlayerDeath(PlayerDeathEvent event) {
                if (event.getEntity().equals(player)) {
                    setAbility2Active(player, false);
                    if (taskId[0] != -1) {
                        Bukkit.getScheduler().cancelTask(taskId[0]);
                    }
                    HandlerList.unregisterAll(this);
                }
            }
        }, plugin);

        return true;
    }
}