package hs.elementPlugin.elements.abilities.impl.water;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Whirlpool - Creates a spinning vortex that makes enemies orbit around the player, dealing damage
 */
public class WaterWhirlpoolAbility extends BaseAbility {
    private final ElementPlugin plugin;
    private final Map<UUID, Double> entityAngles = new HashMap<>();

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
        final double orbitRadius = 5.0;
        final double orbitSpeed = Math.toRadians(5); // 5 degrees per tick = smooth rotation

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
                            100, orbitRadius, 1.0, orbitRadius, 0.5, null, true
                    );

                    // Clean up entity angle tracking
                    entityAngles.clear();

                    cancel();
                    return;
                }

                Location currentCenter = player.getLocation();

                // Find and orbit nearby enemies
                for (LivingEntity entity : currentCenter.getNearbyLivingEntities(orbitRadius + 2)) {
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

                    // Apply pull force when within particle range (orbitRadius + 2)
                    if (distance <= orbitRadius + 2) {
                        // Stronger pull when farther away, weaker when closer
                        double pullStrength = Math.min(distance / orbitRadius, 1.0) * 0.3;

                        Vector pullDirection = currentCenter.toVector().subtract(entityLoc.toVector()).normalize();
                        Vector pullForce = pullDirection.multiply(pullStrength);

                        // Combine with current velocity for smooth pulling
                        Vector currentVelocity = entity.getVelocity();
                        entity.setVelocity(currentVelocity.add(pullForce));
                    }

                    // If entity is within orbit range, make them orbit
                    if (distance <= orbitRadius + 1) {
                        Vector orbitalVelocity = calculateOrbitalVelocity(
                                currentCenter,
                                entity,
                                orbitRadius,
                                orbitSpeed
                        );

                        entity.setVelocity(orbitalVelocity);

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
                }

                // Create whirlpool visual effect
                createWhirlpoolParticles(currentCenter, orbitRadius, currentAngle, ticks);

                // Play sound every second
                if (ticks % 20 == 0) {
                    player.getWorld().playSound(
                            currentCenter,
                            Sound.BLOCK_WATER_AMBIENT,
                            1.0f, 1.2f
                    );
                }

                currentAngle += orbitSpeed;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    /**
     * Calculate orbital velocity to make entity circle around the player
     */
    private Vector calculateOrbitalVelocity(Location center, LivingEntity entity, double targetRadius, double orbitSpeed) {
        Vector toEntity = entity.getLocation().toVector().subtract(center.toVector());
        toEntity.setY(0); // Keep in horizontal plane

        double currentDistance = toEntity.length();

        if (currentDistance < 0.1) {
            // Entity is too close to center, push them out
            double angle = Math.random() * 2 * Math.PI;
            return new Vector(Math.cos(angle), 0, Math.sin(angle)).multiply(0.5);
        }

        // Get or initialize entity's angle
        UUID entityId = entity.getUniqueId();
        if (!entityAngles.containsKey(entityId)) {
            // Initialize angle based on current position
            double angle = Math.atan2(toEntity.getZ(), toEntity.getX());
            entityAngles.put(entityId, angle);
        }

        // Update angle for orbital motion
        double currentAngle = entityAngles.get(entityId);
        double newAngle = currentAngle + orbitSpeed;
        entityAngles.put(entityId, newAngle);

        // Calculate target position on orbit
        double targetX = Math.cos(newAngle) * targetRadius;
        double targetZ = Math.sin(newAngle) * targetRadius;
        Vector targetPos = new Vector(targetX, 0, targetZ);

        // Calculate velocity to move toward target position
        Vector velocity = targetPos.subtract(toEntity);

        // Add radial force to maintain orbit radius
        double radiusError = targetRadius - currentDistance;
        Vector radialForce = toEntity.clone().normalize().multiply(radiusError * 0.15);
        velocity.add(radialForce);

        // Add tangential velocity for circular motion
        Vector tangent = new Vector(-toEntity.getZ(), 0, toEntity.getX()).normalize();
        velocity.add(tangent.multiply(orbitSpeed * targetRadius * 10));

        // AGGRESSIVE GRAVITY: Check for holes every tick and apply strong fall
        Location checkLoc = entity.getLocation().subtract(0, 0.1, 0);
        boolean holeDetected = false;

        // Check multiple blocks below to detect holes faster
        for (int i = 0; i < 3; i++) {
            Location belowLoc = entity.getLocation().subtract(0, i + 0.5, 0);
            if (belowLoc.getBlock().getType().isAir() || !belowLoc.getBlock().getType().isSolid()) {
                holeDetected = true;
                break;
            }
        }

        if (holeDetected) {
            // There's a hole - apply strong downward velocity immediately
            velocity.setY(-1.0);
        } else if (entity.isOnGround()) {
            // Entity is on ground - keep them there
            velocity.setY(-0.1);
        } else {
            // No hole but not on ground - slight downward
            velocity.setY(Math.min(velocity.getY(), 0.1));
        }

        // Normalize and scale to reasonable speed
        double speed = 0.4;
        if (velocity.lengthSquared() > speed * speed) {
            velocity.normalize().multiply(speed);
        }

        return velocity;
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
        return ChatColor.GRAY + "Create a spinning vortex that forces enemies to orbit around you, dealing damage over 8 seconds.";
    }
}