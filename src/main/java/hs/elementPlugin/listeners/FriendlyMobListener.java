package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.impl.FireElement;
import org.bukkit.Bukkit;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class FriendlyMobListener implements Listener {
    private final ElementPlugin plugin;

    public FriendlyMobListener(ElementPlugin plugin) {
        this.plugin = plugin;
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
                                } else if (distance > 3) {
                                    // Follow the owner
                                    mob.setTarget(null);
                                    mob.getPathfinder().moveTo(owner.getLocation(), 1.2);
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
        // Fire friendly blazes: don't target owner
        if (e.getEntity() instanceof Blaze blaze && e.getTarget() instanceof Player target) {
            if (blaze.hasMetadata(FireElement.META_FRIENDLY_BLAZE_OWNER)) {
                try {
                    String ownerStr = blaze.getMetadata(FireElement.META_FRIENDLY_BLAZE_OWNER).get(0).asString();
                    UUID owner = UUID.fromString(ownerStr);
                    if (target.getUniqueId().equals(owner)) {
                        e.setCancelled(true);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }

        // Earth charmed mobs: don't target owner or trusted players; retarget nearest other player
        if (e.getEntity() instanceof Mob mob && e.getTarget() instanceof Player tgt) {
            if (mob.hasMetadata("earth_charmed_owner") && mob.hasMetadata("earth_charmed_until")) {
                try {
                    String ownerStr = mob.getMetadata("earth_charmed_owner").get(0).asString();
                    long until = mob.getMetadata("earth_charmed_until").get(0).asLong();
                    UUID ownerId = UUID.fromString(ownerStr);

                    if (System.currentTimeMillis() > until) return; // expired

                    Player owner = org.bukkit.Bukkit.getPlayer(ownerId);
                    if (owner == null) return;

                    // Don't target owner or their trusted players
                    if (tgt.getUniqueId().equals(ownerId)) {
                        e.setCancelled(true);
                        return;
                    }

                    // Check if target is trusted by owner
                    var trustManager = plugin.getTrustManager();
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
}