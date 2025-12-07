package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Fire element's Hellish Flames ability - sets enemies on fire with inextinguishable flames
 */
public class HellishFlamesAbility extends BaseAbility {
    private final ElementPlugin plugin;

    // Metadata key for tracking hellish flames
    public static final String META_HELLISH_FLAMES = "fire_hellish_flames";

    public HellishFlamesAbility(ElementPlugin plugin) {
        super("fire_hellish_flames", 50, 15, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Location center = player.getLocation();
        double radius = 8.0;
        int durationSeconds = 10;

        // Play activation sound and particles
        player.getWorld().playSound(center, Sound.ENTITY_BLAZE_AMBIENT, 2.0f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_GHAST_SCREAM, 1.5f, 0.8f);

        // Create expanding ring of fire particles
        new BukkitRunnable() {
            double currentRadius = 0;
            int ticks = 0;
            final int maxTicks = 20; // 1 second animation

            @Override
            public void run() {
                if (ticks >= maxTicks || currentRadius > radius) {
                    cancel();
                    return;
                }

                // Ring of fire particles
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    Location particleLoc = center.clone().add(x, 0.5, z);

                    // Flame particles
                    player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 3, 0.2, 0.2, 0.2, 0.05, null, true);

                    // Add lava particles for hellish effect
                    if (ticks % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.LAVA, particleLoc, 1, 0, 0, 0, 0, null, true);
                    }
                }

                // Vertical pillars of fire every few ticks
                if (ticks % 3 == 0) {
                    for (int angle = 0; angle < 360; angle += 45) {
                        double rad = Math.toRadians(angle);
                        double x = Math.cos(rad) * currentRadius;
                        double z = Math.sin(rad) * currentRadius;

                        for (double height = 0; height <= 2.0; height += 0.4) {
                            Location pillarLoc = center.clone().add(x, height, z);
                            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, pillarLoc, 2, 0.1, 0.1, 0.1, 0, null, true);
                        }
                    }
                }

                currentRadius += radius / maxTicks;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Apply hellish flames to nearby enemies
        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(player)) continue;

            // Skip trusted players
            if (entity instanceof Player targetPlayer) {
                if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                    continue;
                }
            }

            applyHellishFlames(entity, durationSeconds);

            // Burst effect on each target
            entity.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    entity.getLocation().add(0, 1, 0),
                    30, 0.3, 0.5, 0.3, 0.1, null, true
            );
            entity.getWorld().spawnParticle(
                    Particle.LAVA,
                    entity.getLocation().add(0, 1, 0),
                    10, 0.3, 0.5, 0.3, 0, null, true
            );
        }

        setActive(player, true);

        // Deactivate after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                setActive(player, false);
            }
        }.runTaskLater(plugin, durationSeconds * 20L);

        return true;
    }

    /**
     * Apply hellish flames to a target that continuously reapply fire
     */
    private void applyHellishFlames(LivingEntity entity, int durationSeconds) {
        long flamesUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        entity.setMetadata(META_HELLISH_FLAMES, new FixedMetadataValue(plugin, flamesUntil));

        // Set initial fire
        entity.setFireTicks(durationSeconds * 20); // 20 ticks per second

        // Task to continuously reapply fire (inextinguishable)
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead()) {
                    entity.removeMetadata(META_HELLISH_FLAMES, plugin);
                    cancel();
                    return;
                }

                if (!entity.hasMetadata(META_HELLISH_FLAMES)) {
                    cancel();
                    return;
                }

                long endTime = entity.getMetadata(META_HELLISH_FLAMES).get(0).asLong();
                if (System.currentTimeMillis() >= endTime) {
                    // Flames expired
                    entity.removeMetadata(META_HELLISH_FLAMES, plugin);
                    entity.setFireTicks(0); // Clear fire
                    cancel();
                    return;
                }

                // Continuously set fire ticks to maximum (inextinguishable)
                entity.setFireTicks(Math.max(entity.getFireTicks(), 40)); // Keep at least 2 seconds of fire

                // Spawn soul fire particles every few ticks for visual effect
                if (ticks % 5 == 0) {
                    entity.getWorld().spawnParticle(
                            Particle.SOUL_FIRE_FLAME,
                            entity.getLocation().add(0, 1, 0),
                            5, 0.2, 0.3, 0.2, 0.02, null, true
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 5L); // Check every 5 ticks (0.25 seconds)
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Hellish Flames";
    }

    @Override
    public String getDescription() {
        return "Set nearby enemies ablaze with inextinguishable hellish flames for 10 seconds.";
    }
}