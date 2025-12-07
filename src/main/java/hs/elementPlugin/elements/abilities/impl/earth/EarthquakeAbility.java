package hs.elementPlugin.elements.abilities.impl.earth;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class EarthquakeAbility extends BaseAbility {

    private final ElementPlugin plugin;

    // Metadata key for earthquake stun tracking
    public static final String META_EARTHQUAKE_STUNNED = "earth_earthquake_stunned";

    public EarthquakeAbility(ElementPlugin plugin) {
        super("earth_earthquake", 75, 15, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Location center = player.getLocation();
        double radius = 10.0;
        int stunDurationSeconds = 3;

        // Play earthquake sounds
        player.getWorld().playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        // Create expanding shockwave effect
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

                // Ground crack particles
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    Location particleLoc = center.clone().add(x, 0.1, z);

                    // Block crack particles
                    player.getWorld().spawnParticle(
                            Particle.BLOCK,
                            particleLoc,
                            5, 0.2, 0.1, 0.2, 0.1,
                            Material.STONE.createBlockData(),
                            true
                    );

                    // Dust clouds
                    if (ticks % 2 == 0) {
                        player.getWorld().spawnParticle(
                                Particle.CAMPFIRE_COSY_SMOKE,
                                particleLoc,
                                2, 0.1, 0.1, 0.1, 0.02,
                                null, true
                        );
                    }
                }

                // Ground shake effect - vertical pillars
                if (ticks % 3 == 0) {
                    for (int angle = 0; angle < 360; angle += 45) {
                        double rad = Math.toRadians(angle);
                        double x = Math.cos(rad) * currentRadius;
                        double z = Math.sin(rad) * currentRadius;

                        for (double height = 0; height <= 1.5; height += 0.3) {
                            Location pillarLoc = center.clone().add(x, height, z);
                            player.getWorld().spawnParticle(
                                    Particle.BLOCK,
                                    pillarLoc,
                                    3, 0.1, 0.1, 0.1, 0,
                                    Material.COBBLESTONE.createBlockData(),
                                    true
                            );
                        }
                    }
                }

                currentRadius += radius / maxTicks;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Stun nearby entities
        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(player)) continue;

            // Skip trusted players
            if (entity instanceof Player targetPlayer) {
                if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                    continue;
                }
            }

            stunEntity(entity, stunDurationSeconds);

            // Earthquake hit effect
            entity.getWorld().spawnParticle(
                    Particle.BLOCK,
                    entity.getLocation().add(0, 1, 0),
                    30, 0.3, 0.5, 0.3, 0.1,
                    Material.DIRT.createBlockData(),
                    true
            );

            entity.getWorld().playSound(
                    entity.getLocation(),
                    Sound.ENTITY_PLAYER_HURT,
                    1.0f, 0.8f
            );
        }

        setActive(player, true);

        // Deactivate after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                setActive(player, false);
            }
        }.runTaskLater(plugin, stunDurationSeconds * 20L);

        return true;
    }

    /**
     * Stun an entity for a duration
     */
    private void stunEntity(LivingEntity entity, int durationSeconds) {
        long stunUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        entity.setMetadata(META_EARTHQUAKE_STUNNED, new FixedMetadataValue(plugin, stunUntil));

        // Disable mob AI if it's a mob
        if (entity instanceof Mob mob) {
            mob.setAware(false);

            // Re-enable AI after stun expires
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mob.isValid()) {
                        mob.setAware(true);
                    }
                }
            }.runTaskLater(plugin, durationSeconds * 20L);
        }

        // Visual stun effect - periodic particles
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || ticks >= maxTicks) {
                    entity.removeMetadata(META_EARTHQUAKE_STUNNED, plugin);
                    cancel();
                    return;
                }

                if (!entity.hasMetadata(META_EARTHQUAKE_STUNNED)) {
                    cancel();
                    return;
                }

                long end = entity.getMetadata(META_EARTHQUAKE_STUNNED).get(0).asLong();
                if (System.currentTimeMillis() >= end) {
                    entity.removeMetadata(META_EARTHQUAKE_STUNNED, plugin);
                    cancel();
                    return;
                }

                // Show stun particles around head
                if (ticks % 5 == 0) {
                    entity.getWorld().spawnParticle(
                            Particle.CRIT,
                            entity.getEyeLocation(),
                            5, 0.3, 0.3, 0.3, 0,
                            null, true
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public String getName() {
        return ChatColor.GOLD + "Earthquake";
    }

    @Override
    public String getDescription() {
        return "Create a powerful earthquake that stuns all enemies within 10 blocks for 3 seconds.";
    }
}