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
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
        
        if (upgradeLevel >= 2) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 4, true, false));
        }
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        Location playerLoc = player.getLocation();

        boolean foundTargets = false;
        for (LivingEntity entity : playerLoc.getNearbyLivingEntities(5.0)) {
            if (entity.equals(player)) continue;
            if (!isValidTarget(context, entity)) continue;
            
            foundTargets = true;
            final LivingEntity target = entity;
            final double startY = target.getLocation().getY();

            Location targetLoc = target.getLocation();
            Location groundLoc = new Location(targetLoc.getWorld(), targetLoc.getX(), startY, targetLoc.getZ());
            
            targetLoc.getWorld().playSound(groundLoc, Sound.BLOCK_WATER_AMBIENT, 1.0f, 0.5f);
            targetLoc.getWorld().spawnParticle(Particle.SPLASH, groundLoc, 30, 0.5, 0.1, 0.5, 0.3);
            
            new BukkitRunnable() {
                int ticks = 0;
                double geyserHeight = 0;
                double lastGeyserHeight = 0;
                
                @Override
                public void run() {
                    if (target.isDead() || !target.isValid()) { cancel(); return; }
                    Location loc = target.getLocation();
                    target.setVelocity(new Vector(0, 1.2, 0));
                    
                    target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc.getX(), loc.getY() - 0.01, loc.getZ(), 5, 0.2, 0.0, 0.2, 0.01);
                    
                    Location groundLoc = new Location(loc.getWorld(), loc.getX(), startY, loc.getZ());
                    
                    // Smooth height transition logic
                    double targetHeight = Math.min(loc.getY() - startY, 20);
                    double heightDiff = targetHeight - lastGeyserHeight;
                    geyserHeight = lastGeyserHeight + Math.min(heightDiff, 0.5);
                    lastGeyserHeight = geyserHeight;
                    
                    // Create particles along the geyser column
                    for (double y = 0; y <= geyserHeight; y += 0.25) {
                        if (y % 0.5 != 0 && y > 1) continue;
                        
                        Location particleLoc = groundLoc.clone().add(0, y, 0);
                        // Wider spread at bottom, narrower at top
                        double spread = 0.15 * (1 - (y / Math.max(geyserHeight, 1)));
                        
                        target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, particleLoc, 1, spread, 0.05, spread, 0.01);
                        
                        if (y % 1.5 < 0.25 && y > 0.5) {
                            target.getWorld().spawnParticle(Particle.UNDERWATER, particleLoc, 1, spread, 0.05, spread, 0.01);
                        }
                    }
                    
                    if (ticks % 3 == 0) {
                        target.getWorld().spawnParticle(Particle.SPLASH, groundLoc, 3, 0.2, 0.05, 0.2, 0.05);
                    }
                    
                    ticks++;
                    if (loc.getY() - startY >= 20 || ticks >= 25) {
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

        setAbility2Active(player, true);

        final int[] taskId = {-1};

        BukkitRunnable task = new BukkitRunnable() {
            int ticks = 0;
            double totalDamageDealt = 0;
            @Override
            public void run() {
                // Ability ends after 10 seconds or 5 hearts of damage
                if (!player.isOnline() || ticks >= 200 || totalDamageDealt >= 10) {
                    setAbility2Active(player, false);
                    cancel();
                    return;
                }
                Vector dir = player.getLocation().getDirection().normalize();

                // Apply damage every 0.25 seconds
                if (ticks % 5 == 0) {
                    Location chestLoc = player.getLocation().add(0, 1.2, 0);
                    RayTraceResult r = player.getWorld().rayTraceEntities(chestLoc, dir, 20.0,
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
                                    plugin.getLogger().warning("Error spawning water beam particles: " + e.getMessage());
                                }
                            }
                        }
                    }
                }

                try {
                    // Render beam particles every other tick to prevent flashing
                    if (ticks % 2 == 0) {
                        Location chestLoc = player.getLocation().add(0, 1.2, 0);
                        for (double d = 0; d <= 20; d += 0.5) {
                            Location pt = chestLoc.clone().add(dir.clone().multiply(d));
                            player.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, pt, 1, 0.0, 0.0, 0.0, 0.0);
                            if (d % 2.0 < 0.5) {
                                player.getWorld().spawnParticle(Particle.DRIPPING_WATER, pt, 1, 0.0, 0.0, 0.0, 0.0);
                            }
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error spawning water beam particles: " + e.getMessage());
                }

                ticks++;
            }
        };

        taskId[0] = task.runTaskTimer(plugin, 0L, 1L).getTaskId();

        // Cleanup listener for when player dies or logs off
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