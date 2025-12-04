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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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

        // Play explosion sound
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);

        // Create expanding ice spike/burst pattern (different from air blast)
        new BukkitRunnable() {
            double currentRadius = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (currentRadius >= radius || ticks >= 15) {
                    cancel();
                    return;
                }

                // Create multiple expanding spike rings at different heights
                for (double height = 0; height <= 1.5; height += 0.5) {
                    // Outer ring of ice spikes
                    for (int i = 0; i < 360; i += 8) {
                        double rad = Math.toRadians(i);
                        double x = Math.cos(rad) * currentRadius;
                        double z = Math.sin(rad) * currentRadius;

                        Location particleLoc = center.clone().add(x, height, z);

                        // Dense ice/snow particles forming spikes
                        player.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 5, 0.1, 0.2, 0.1, 0, null, true);
                        player.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 2, 0.05, 0.1, 0.05, 0.02, null, true);
                    }
                }

                // Inner spiral of ice crystals
                for (int i = 0; i < 8; i++) {
                    double angle = Math.toRadians(i * 45 + ticks * 30);
                    double spiralRadius = currentRadius * 0.6;
                    double x = Math.cos(angle) * spiralRadius;
                    double z = Math.sin(angle) * spiralRadius;

                    Location spiralLoc = center.clone().add(x, 0.5, z);
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, spiralLoc, 8, 0.2, 0.3, 0.2, 0.05, null, true);
                }

                // Ground frost effect
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    Location groundLoc = center.clone().add(x, 0.05, z);
                    player.getWorld().spawnParticle(Particle.CLOUD, groundLoc, 2, 0.2, 0.05, 0.2, 0, null, true);
                }

                currentRadius += 0.5;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Freeze nearby enemies
        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(player)) continue;

            // Check if entity is a player and if they're trusted
            if (entity instanceof Player targetPlayer) {
                if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                    continue;
                }
            }

            // Apply freeze effect
            freezeEntity(entity, 5); // 5 seconds freeze

            // Visual feedback - ice crystals bursting from entity
            entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0),
                    30, 0.4, 0.6, 0.4, 0.05, null, true);
            entity.getWorld().spawnParticle(Particle.FIREWORK, entity.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0.02, null, true);
        }

        return true;
    }

    /**
     * Freeze an entity, preventing movement
     */
    private void freezeEntity(LivingEntity entity, int durationSeconds) {
        long freezeUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        entity.setMetadata(META_NOVA_FROZEN, new FixedMetadataValue(plugin, freezeUntil));

        // Apply visual freeze
        entity.setFreezeTicks(entity.getMaxFreezeTicks());

        // Apply Slowness IV
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20, 3, false, false, false));

        // Disable AI for mobs
        if (entity instanceof Mob mob) {
            mob.setAware(false);

            // Re-enable AI after freeze
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mob.isValid()) {
                        mob.setAware(true);
                    }
                }
            }.runTaskLater(plugin, durationSeconds * 20L);
        }

        // Cleanup metadata after duration
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
        return "Create an explosion of ice around you, freezing the ground and nearby enemies for 5 seconds.";
    }
}