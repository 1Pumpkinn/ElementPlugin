package hs.elementPlugin.listeners;

import hs.elementPlugin.elements.impl.FireElement;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.metadata.MetadataValue;

import java.util.UUID;

public class FriendlyMobListener implements Listener {
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
        // Earth charmed mobs: don't target owner; retarget nearest other player if possible
        if (e.getEntity() instanceof org.bukkit.entity.Mob mob && e.getTarget() instanceof Player tgt) {
            if (mob.hasMetadata("earth_charmed_owner") && mob.hasMetadata("earth_charmed_until")) {
                try {
                    String ownerStr = mob.getMetadata("earth_charmed_owner").get(0).asString();
                    long until = mob.getMetadata("earth_charmed_until").get(0).asLong();
                    UUID owner = UUID.fromString(ownerStr);
                    if (System.currentTimeMillis() > until) return; // expired
                    if (tgt.getUniqueId().equals(owner)) {
                        // find another player
                        Player nearest = null;
                        double best = Double.MAX_VALUE;
                        for (Player p : mob.getWorld().getPlayers()) {
                            if (p.getUniqueId().equals(owner)) continue;
                            double d = p.getLocation().distanceSquared(mob.getLocation());
                            if (d < best && d < 16*16) { best = d; nearest = p; }
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