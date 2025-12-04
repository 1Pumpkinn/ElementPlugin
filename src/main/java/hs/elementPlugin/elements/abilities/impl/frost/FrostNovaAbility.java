package hs.elementPlugin.elements.abilities.impl.frost;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class FrostNovaAbility extends BaseAbility {

    private final ElementPlugin plugin;
    public static final String META_NOVA_FROZEN = "frost_nova_frozen";

    public FrostNovaAbility(ElementPlugin plugin) {
        super("frost_nova", 50, 10, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Location center = player.getLocation();
        double radius = 6.0;

        // Sounds
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);

        // Expanding frost ring animation
        new BukkitRunnable() {
            double currentRadius = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (currentRadius >= radius || ticks >= 20) {
                    cancel();
                    return;
                }

                // Main ring
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    Location particleLoc = center.clone().add(x, 0.1, z);

                    player.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 1, 0, 0, 0, 0);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 2, 0.05, 0.05, 0.05, 0.01);
                }

                // Vertical ice spikes
                if (ticks % 3 == 0) {
                    for (int angle = 0; angle < 360; angle += 45) {
                        double rad = Math.toRadians(angle);
                        double x = Math.cos(rad) * currentRadius;
                        double z = Math.sin(rad) * currentRadius;

                        for (double height = 0; height <= 1.2; height += 0.25) {
                            Location spikeLoc = center.clone().add(x, height, z);
                            player.getWorld().spawnParticle(Particle.FIREWORK, spikeLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                // Ambient frost mist
                player.getWorld().spawnParticle(
                        Particle.SNOWFLAKE,
                        center.clone().add(0, 0.2 + (currentRadius / 12), 0),
                        10,
                        currentRadius / 7,
                        0.25,
                        currentRadius / 7,
                        0.01
                );

                currentRadius += 0.4;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Apply freeze
        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(player)) continue;

            // Skip trusted players
            if (entity instanceof Player targetPlayer) {
                if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                    continue;
                }
            }

            freezeEntity(entity, 5);

            // Visual burst on hit
            entity.getWorld().spawnParticle(Particle.FIREWORK, entity.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0.02);
        }

        return true;
    }

    // =====================================================
    // FREEZE FUNCTION (5 seconds guaranteed)
    // =====================================================
    private void freezeEntity(LivingEntity entity, int durationSeconds) {
        long freezeUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        entity.setMetadata(META_NOVA_FROZEN, new FixedMetadataValue(plugin, freezeUntil));

        // Maintain freeze ticks visually for entire duration
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid() || !entity.hasMetadata(META_NOVA_FROZEN)) {
                    cancel();
                    return;
                }

                long end = entity.getMetadata(META_NOVA_FROZEN).get(0).asLong();
                if (System.currentTimeMillis() >= end) {
                    cancel();
                    return;
                }

                entity.setFreezeTicks(20 * durationSeconds);
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Disable mob AI
        if (entity instanceof Mob mob) {
            mob.setAware(false);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mob.isValid()) {
                        mob.setAware(true);
                    }
                }
            }.runTaskLater(plugin, durationSeconds * 20L);
        }

        // Remove frozen metadata
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isValid()) {
                    entity.removeMetadata(META_NOVA_FROZEN, plugin);
                }
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Frost Nova";
    }

    @Override
    public String getDescription() {
        return "Create an explosion of ice, freezing enemies for 5 seconds.";
    }
}
