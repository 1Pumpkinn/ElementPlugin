package hs.elementPlugin.elements.impl.death.listeners;

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

public class DeathFriendlyMobListener implements Listener {
    private final ElementPlugin plugin;
    private final TrustManager trustManager;

    public DeathFriendlyMobListener(ElementPlugin plugin, TrustManager trustManager) {
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
                        if (!mob.hasMetadata("death_summoned_owner") || !mob.hasMetadata("death_summoned_until")) {
                            continue;
                        }

                        try {
                            long until = mob.getMetadata("death_summoned_until").get(0).asLong();
                            if (System.currentTimeMillis() > until) {
                                mob.removeMetadata("death_summoned_owner", plugin);
                                mob.removeMetadata("death_summoned_until", plugin);
                                continue;
                            }

                            String ownerStr = mob.getMetadata("death_summoned_owner").get(0).asString();
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
        // Death summoned undead: don't target owner or trusted players
        if (e.getEntity() instanceof Mob mob && e.getTarget() instanceof Player target) {
            if (mob.hasMetadata("death_summoned_owner") && mob.hasMetadata("death_summoned_until")) {
                try {
                    String ownerStr = mob.getMetadata("death_summoned_owner").get(0).asString();
                    long until = mob.getMetadata("death_summoned_until").get(0).asLong();
                    UUID ownerId = UUID.fromString(ownerStr);

                    if (System.currentTimeMillis() > until) return; // expired

                    // Don't target owner
                    if (target.getUniqueId().equals(ownerId)) {
                        e.setCancelled(true);
                        return;
                    }

                    // Don't target trusted players
                    if (trustManager.isTrusted(ownerId, target.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        // Prevent death summoned undead from damaging their owner or trusted players
        if (e.getDamager() instanceof Mob mob && e.getEntity() instanceof Player target) {
            if (mob.hasMetadata("death_summoned_owner") && mob.hasMetadata("death_summoned_until")) {
                try {
                    String ownerStr = mob.getMetadata("death_summoned_owner").get(0).asString();
                    long until = mob.getMetadata("death_summoned_until").get(0).asLong();
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
