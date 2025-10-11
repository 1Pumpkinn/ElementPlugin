package hs.elementPlugin.elements.impl.earth.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class EarthFriendlyMobListener implements Listener {
    private final ElementPlugin plugin;
    private final TrustManager trustManager;

    public EarthFriendlyMobListener(ElementPlugin plugin, TrustManager trustManager) {
        this.plugin = plugin;
        this.trustManager = trustManager;
        startFollowTask();
    }

    private void startFollowTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (org.bukkit.World world : Bukkit.getWorlds()) {
                    for (Mob mob : world.getEntitiesByClass(Mob.class)) {
                        if (!mob.hasMetadata("earth_charmed_owner") || !mob.hasMetadata("earth_charmed_until")) {
                            continue;
                        }

                        try {
                            long until = mob.getMetadata("earth_charmed_until").get(0).asLong();
                            if (System.currentTimeMillis() > until) {
                                mob.removeMetadata("earth_charmed_owner", plugin);
                                mob.removeMetadata("earth_charmed_until", plugin);
                                continue;
                            }

                            String ownerStr = mob.getMetadata("earth_charmed_owner").get(0).asString();
                            UUID ownerId = UUID.fromString(ownerStr);
                            Player owner = Bukkit.getPlayer(ownerId);

                            if (owner != null && owner.isOnline()) {
                                double distance = mob.getLocation().distance(owner.getLocation());

                                // If too far, teleport closer
                                if (distance > 30) {
                                    mob.teleport(owner.getLocation());
                                } else {
                                    // Look for enemies to attack first
                                    Player nearestEnemy = null;
                                    double bestDistance = Double.MAX_VALUE;
                                    
                                    for (Player player : mob.getWorld().getPlayers()) {
                                        if (player.getUniqueId().equals(ownerId)) continue;
                                        if (trustManager.isTrusted(ownerId, player.getUniqueId())) continue;
                                        
                                        double playerDistance = mob.getLocation().distanceSquared(player.getLocation());
                                        if (playerDistance < bestDistance && playerDistance < 16*16) { // 16 block range
                                            bestDistance = playerDistance;
                                            nearestEnemy = player;
                                        }
                                    }
                                    
                                    if (nearestEnemy != null) {
                                        // Attack the nearest enemy
                                        mob.setTarget(nearestEnemy);
                                    } else if (distance > 3) {
                                        // No enemies nearby, follow the owner
                                        mob.setTarget(null);
                                        mob.getPathfinder().moveTo(owner.getLocation(), 1.2);
                                    }
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 10L); // Run every 0.5 seconds
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        // Earth charmed mobs: don't target owner or trusted players; retarget nearest other player
        if (e.getEntity() instanceof Mob mob && e.getTarget() instanceof Player tgt) {
            if (mob.hasMetadata("earth_charmed_owner") && mob.hasMetadata("earth_charmed_until")) {
                try {
                    String ownerStr = mob.getMetadata("earth_charmed_owner").get(0).asString();
                    long until = mob.getMetadata("earth_charmed_until").get(0).asLong();
                    UUID ownerId = UUID.fromString(ownerStr);

                    if (System.currentTimeMillis() > until) return; // expired

                    Player owner = Bukkit.getPlayer(ownerId);
                    if (owner == null) return;

                    // Don't target owner
                    if (tgt.getUniqueId().equals(ownerId)) {
                        e.setCancelled(true);
                        return;
                    }

                    // Don't target trusted players
                    if (trustManager.isTrusted(ownerId, tgt.getUniqueId())) {
                        // Find another player that isn't trusted
                        Player nearest = null;
                        double best = Double.MAX_VALUE;
                        for (Player p : mob.getWorld().getPlayers()) {
                            if (p.getUniqueId().equals(ownerId)) continue;
                            if (trustManager.isTrusted(ownerId, p.getUniqueId())) continue;
                            double d = p.getLocation().distanceSquared(mob.getLocation());
                            if (d < best && d < 16*16) {
                                best = d;
                                nearest = p;
                            }
                        }
                        if (nearest != null) {
                            e.setTarget(nearest);
                        } else {
                            e.setCancelled(true);
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        // Prevent charmed mobs from damaging their owner or trusted players
        if (e.getDamager() instanceof Mob mob && e.getEntity() instanceof Player target) {
            if (mob.hasMetadata("earth_charmed_owner") && mob.hasMetadata("earth_charmed_until")) {
                try {
                    String ownerStr = mob.getMetadata("earth_charmed_owner").get(0).asString();
                    long until = mob.getMetadata("earth_charmed_until").get(0).asLong();
                    UUID ownerId = UUID.fromString(ownerStr);

                    if (System.currentTimeMillis() > until) return; // expired

                    // Don't damage owner
                    if (target.getUniqueId().equals(ownerId)) {
                        e.setCancelled(true);
                        return;
                    }

                    // Don't damage trusted players
                    if (trustManager.isTrusted(ownerId, target.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}
