package hs.elementPlugin.elements.abilities.impl.water;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Whirlpool - Creates a spinning vortex that pulls enemies in a circle, dealing damage
 */
public class WaterWhirlpoolAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public WaterWhirlpoolAbility(ElementPlugin plugin) {
        super("water_whirlpool", 50, 20, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Location center = player.getLocation();

        // Play activation sound
        player.getWorld().playSound(center, Sound.ENTITY_PLAYER_SPLASH, 2.0f, 0.5f);
        player.getWorld().playSound(center, Sound.BLOCK_WATER_AMBIENT, 2.0f, 0.8f);

        setActive(player, true);

        // Whirlpool duration: 8 seconds
        final int durationTicks = 160;
        final double radius = 6.0;
        final double spinSpeed = 0.15; // Radians per tick

        new BukkitRunnable() {
            int ticks = 0;
            double currentAngle = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= durationTicks) {
                    setActive(player, false);

                    // Final splash effect
                    player.getWorld().spawnParticle(
                            Particle.SPLASH,
                            center.clone().add(0, 0.5, 0),
                            100, radius, 1.0, radius, 0.5, null, true
                    );

                    cancel();
                    return;
                }

                Location currentCenter = player.getLocation();

                // Find and spin nearby enemies
                for (LivingEntity entity : currentCenter.getNearbyLivingEntities(radius)) {
                    if (entity.equals(player)) continue;

                    // Don't affect trusted players
                    if (entity instanceof Player targetPlayer) {
                        if (context.getTrustManager().isTrusted(
                                player.getUniqueId(),
                                targetPlayer.getUniqueId()
                        )) {
                            continue;
                        }
                    }

                    Location entityLoc = entity.getLocation();
                    double distance = entityLoc.distance(currentCenter);

                    // Calculate tangential velocity (spinning motion)
                    Vector toEntity = entityLoc.toVector().subtract(currentCenter.toVector());
                    toEntity.setY(0); // Keep in horizontal plane

                    if (toEntity.lengthSquared() < 0.01) continue;

                    // Perpendicular vector for spinning
                    Vector tangent = new Vector(-toEntity.getZ(), 0, toEntity.getX()).normalize();

                    // Pull inward + spin
                    Vector pullIn = toEntity.normalize().multiply(-0.3);
                    Vector spin = tangent.multiply(spinSpeed * distance);

                    // FIXED: Keep entities grounded - no upward component
                    // Check if entity is on ground, if so keep them on ground
                    Vector finalVelocity;
                    if (entity.isOnGround()) {
                        // On ground - only horizontal movement
                        finalVelocity = pullIn.add(spin);
                        finalVelocity.setY(-0.1); // Slight downward force to keep grounded
                    } else {
                        // In air - allow slight upward movement but cap it
                        finalVelocity = pullIn.add(spin);
                        finalVelocity.setY(Math.min(finalVelocity.getY(), 0.1)); // Cap upward velocity
                    }

                    entity.setVelocity(finalVelocity);

                    // Deal damage every 10 ticks (0.5 seconds)
                    if (ticks % 10 == 0) {
                        entity.damage(1.0, player);

                        // Splash effect on hit
                        entity.getWorld().spawnParticle(
                                Particle.SPLASH,
                                entity.getLocation().add(0, 1, 0),
                                10, 0.3, 0.3, 0.3, 0.1, null, true
                        );
                    }
                }

                // Create whirlpool visual effect
                createWhirlpoolParticles(currentCenter, radius, currentAngle, ticks);

                // Play sound every second
                if (ticks % 20 == 0) {
                    player.getWorld().playSound(
                            currentCenter,
                            Sound.BLOCK_WATER_AMBIENT,
                            1.0f, 1.2f
                    );
                }

                currentAngle += spinSpeed;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private void createWhirlpoolParticles(Location center, double radius, double angle, int tick) {
        World world = center.getWorld();

        // Create spiral pattern
        int spirals = 3;
        int pointsPerSpiral = 20;

        for (int spiral = 0; spiral < spirals; spiral++) {
            double spiralOffset = (2 * Math.PI * spiral) / spirals;

            for (int i = 0; i < pointsPerSpiral; i++) {
                double t = (double) i / pointsPerSpiral;
                double currentRadius = radius * t;
                double currentAngle = angle + (t * 4 * Math.PI) + spiralOffset;

                double x = Math.cos(currentAngle) * currentRadius;
                double z = Math.sin(currentAngle) * currentRadius;
                double y = 0.2 + (t * 0.5); // Rising spiral

                Location particleLoc = center.clone().add(x, y, z);

                world.spawnParticle(
                        Particle.SPLASH,
                        particleLoc,
                        1, 0.1, 0.1, 0.1, 0.0, null, true
                );

                // Add water droplets
                if (Math.random() < 0.3) {
                    world.spawnParticle(
                            Particle.DRIPPING_WATER,
                            particleLoc,
                            1, 0.0, 0.0, 0.0, 0.0, null, true
                    );
                }
            }
        }

        // Add central column of bubbles
        if (tick % 3 == 0) {
            for (double y = 0; y < 2.0; y += 0.3) {
                world.spawnParticle(
                        Particle.BUBBLE_POP,
                        center.clone().add(0, y, 0),
                        3, 0.2, 0.1, 0.2, 0.0, null, true
                );
            }
        }

        // Ground water ring
        if (tick % 2 == 0) {
            for (int i = 0; i < 360; i += 10) {
                double rad = Math.toRadians(i);
                double x = Math.cos(rad) * radius;
                double z = Math.sin(rad) * radius;

                world.spawnParticle(
                        Particle.FALLING_WATER,
                        center.clone().add(x, 0.1, z),
                        1, 0.0, 0.0, 0.0, 0.0, null, true
                );
            }
        }
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Whirlpool";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Create a spinning vortex that pulls enemies in circles, dealing damage over 8 seconds.";
    }
}