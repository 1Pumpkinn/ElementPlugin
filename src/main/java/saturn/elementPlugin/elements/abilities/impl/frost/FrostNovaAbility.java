package saturn.elementPlugin.elements.abilities.impl.frost;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.abilities.BaseAbility;
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
        int durationSeconds = 5;

        // --- SOUND EFFECTS ---
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f);

        // --- EXPANDING RING EFFECT (lasts 1 second for visual effect) ---
        new BukkitRunnable() {
            double currentRadius = 0;
            int ticks = 0;
            final int maxTicks = 20; // Only 1 second for the expanding ring animation

            @Override
            public void run() {
                if (ticks >= maxTicks || currentRadius > radius) {
                    cancel();
                    return;
                }

                // Main ring
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    Location particleLoc = center.clone().add(x, 0.1, z);

                    // Snowflake particles
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 2, 0.1, 0.1, 0.1, 0);
                    // Add some sparkle with fireworks
                    if (ticks % 2 == 0) {
                        player.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 1, 0, 0, 0, 0);
                    }
                }

                // Vertical ice spikes every few ticks
                if (ticks % 3 == 0) {
                    for (int angle = 0; angle < 360; angle += 30) {
                        double rad = Math.toRadians(angle);
                        double x = Math.cos(rad) * currentRadius;
                        double z = Math.sin(rad) * currentRadius;

                        for (double height = 0; height <= 1.5; height += 0.3) {
                            Location spikeLoc = center.clone().add(x, height, z);
                            player.getWorld().spawnParticle(Particle.SNOWFLAKE, spikeLoc, 1, 0, 0, 0, 0);
                        }
                    }
                }

                // Expand outward faster to reach full radius in 1 second
                currentRadius += radius / maxTicks;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // --- FREEZE NEARBY ENTITIES ---
        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(player)) continue;

            // Skip trusted players
            if (entity instanceof Player targetPlayer) {
                if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                    continue;
                }
            }

            freezeEntity(entity, durationSeconds);

            // Burst effect on each frozen target
            entity.getWorld().spawnParticle(
                    Particle.SNOWFLAKE,
                    entity.getLocation().add(0, 1, 0),
                    30, 0.3, 0.5, 0.3, 0.05
            );
            // Add ice block particles
            entity.getWorld().spawnParticle(
                    Particle.BLOCK,
                    entity.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0.05,
                    org.bukkit.Material.ICE.createBlockData()
            );
        }

        return true;
    }

    /**
     * Freezes a target for exactly X seconds.
     */
    private void freezeEntity(LivingEntity entity, int durationSeconds) {
        long freezeUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        entity.setMetadata(META_NOVA_FROZEN, new FixedMetadataValue(plugin, freezeUntil));

        // Store the task ID so we can cancel it properly
        final BukkitRunnable freezeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!entity.isValid()) {
                    cancel();
                    return;
                }

                if (!entity.hasMetadata(META_NOVA_FROZEN)) {
                    entity.setFreezeTicks(0);
                    cancel();
                    return;
                }

                long end = entity.getMetadata(META_NOVA_FROZEN).get(0).asLong();
                if (System.currentTimeMillis() >= end) {
                    // Freeze expired - cleanup and allow movement
                    entity.removeMetadata(META_NOVA_FROZEN, plugin);
                    entity.setFreezeTicks(0);
                    cancel();
                    return;
                }

                // Keep entity frozen visually
                entity.setFreezeTicks(140);
            }
        };
        freezeTask.runTaskTimer(plugin, 0L, 1L);

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
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Frost Nova";
    }

    @Override
    public String getDescription() {
        return "Unleash a blast of freezing wind, stunning nearby enemies in ice for 5 seconds.";
    }
}