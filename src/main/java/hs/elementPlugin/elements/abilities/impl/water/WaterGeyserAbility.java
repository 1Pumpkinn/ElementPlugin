package hs.elementPlugin.elements.abilities.impl.water;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Water element's geyser ability that launches entities upward
 */
public class WaterGeyserAbility extends BaseAbility {

    private final hs.elementPlugin.ElementPlugin plugin;

    public WaterGeyserAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("water_geyser", 75, 5, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
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
            // Use force=true to ensure particles are visible at longer distances
            targetLoc.getWorld().spawnParticle(Particle.SPLASH, groundLoc, 30, 0.5, 0.1, 0.5, 0.3, null, true);

            new BukkitRunnable() {
                int ticks = 0;
                double geyserHeight = 0;
                double lastGeyserHeight = 0;

                @Override
                public void run() {
                    if (target.isDead() || !target.isValid()) { cancel(); return; }
                    Location loc = target.getLocation();
                    // Limit height to 20 blocks maximum
                    double currentHeight = target.getLocation().getY() - startY;
                    if (currentHeight < 20) {
                        target.setVelocity(new Vector(0, 0.8, 0));
                    } else {
                        target.setVelocity(new Vector(0, 0, 0)); // Stop upward movement at 20 blocks
                    }

                    // Force particles to be visible
                    target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, loc.getX(), loc.getY() - 0.01, loc.getZ(), 5, 0.2, 0.0, 0.2, 0.01, null, true);

                    Location groundLoc = new Location(loc.getWorld(), loc.getX(), startY, loc.getZ());

                    // Smooth height transition logic - launch to 20 blocks
                    double targetHeight = Math.min(loc.getY() - startY, 20);
                    double heightDiff = targetHeight - lastGeyserHeight;
                    geyserHeight = lastGeyserHeight + Math.min(heightDiff, 0.5);
                    lastGeyserHeight = geyserHeight;

                    // Create particles along the geyser column with force=true
                    for (double y = 0; y <= geyserHeight; y += 0.25) {
                        if (y % 0.5 != 0 && y > 1) continue;

                        Location particleLoc = groundLoc.clone().add(0, y, 0);
                        // Wider spread at bottom, narrower at top
                        double spread = 0.15 * (1 - (y / Math.max(geyserHeight, 1)));

                        target.getWorld().spawnParticle(Particle.BUBBLE_COLUMN_UP, particleLoc, 1, spread, 0.05, spread, 0.01, null, true);

                        if (y % 1.5 < 0.25 && y > 0.5) {
                            target.getWorld().spawnParticle(Particle.UNDERWATER, particleLoc, 1, spread, 0.05, spread, 0.01, null, true);
                        }
                    }

                    ticks++;
                    if (ticks >= 40) {
                        cancel();
                    }
                }
            }.runTaskTimer(context.getPlugin(), 0L, 1L);
        }

        if (!foundTargets) {
            player.sendMessage("§cNo valid targets found!");
            return false;
        }

        return true;
    }

    @Override
    public String getName() {
        return "Water Geyser";
    }

    @Override
    public String getDescription() {
        return "Launches nearby enemies upward with a powerful geyser.";
    }

    @Override
    protected boolean isValidTarget(ElementContext context, LivingEntity entity) {
        // Check if entity is a valid target (not a friendly player, etc.)
        if (entity instanceof Player targetPlayer) {
            // Don't target trusted players
            if (context.getPlugin().getTrustManager().isTrusted(context.getPlayer().getUniqueId(), targetPlayer.getUniqueId())) {
                return false;
            }
        }
        return true;
    }
}