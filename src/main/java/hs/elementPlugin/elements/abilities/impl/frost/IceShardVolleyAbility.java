package hs.elementPlugin.elements.abilities.impl.frost;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class IceShardVolleyAbility extends BaseAbility implements Listener {
    private final ElementPlugin plugin;
    private final Set<UUID> iceShardProjectiles = new HashSet<>();

    public static final String META_ICE_SHARD = "ice_shard_projectile";
    public static final String META_SHOOTER = "ice_shard_shooter";

    public IceShardVolleyAbility(ElementPlugin plugin) {
        super("ice_shard_volley", 75, 10, 2);
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Vector direction = player.getLocation().getDirection().normalize();

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 0.8f);

        // Fire 5 ice shards in a cone pattern
        double coneAngle = 20.0; // degrees
        int shardCount = 5;

        for (int i = 0; i < shardCount; i++) {
            // Calculate spread angle (-10 to +10 degrees)
            double angleOffset = ((i - 2) * coneAngle) / (shardCount - 1);

            // Create rotated direction vector
            Vector shardDirection = rotateAroundY(direction.clone(), Math.toRadians(angleOffset));

            // Small delay between shots for visual effect
            int delay = i * 2; // 2 ticks between each shard

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        fireIceShard(player, shardDirection, context);
                    }
                }
            }.runTaskLater(plugin, delay);
        }

        return true;
    }

    /**
     * Fire a single ice shard
     */
    private void fireIceShard(Player player, Vector direction, ElementContext context) {
        Location spawnLoc = player.getEyeLocation().add(direction.clone().multiply(0.5));

        // Use snowball as projectile
        Snowball shard = player.getWorld().spawn(spawnLoc, Snowball.class);
        shard.setVelocity(direction.multiply(2.0));
        shard.setShooter(player);

        // Mark as ice shard
        shard.setMetadata(META_ICE_SHARD, new FixedMetadataValue(plugin, true));
        shard.setMetadata(META_SHOOTER, new FixedMetadataValue(plugin, player.getUniqueId()));
        iceShardProjectiles.add(shard.getUniqueId());

        // Play shoot sound
        player.getWorld().playSound(spawnLoc, Sound.ENTITY_SNOWBALL_THROW, 0.5f, 1.5f);

        // Create particle trail
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!shard.isValid() || shard.isDead() || ticks >= 100) {
                    iceShardProjectiles.remove(shard.getUniqueId());
                    cancel();
                    return;
                }

                Location loc = shard.getLocation();

                // Ice particle trail
                shard.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 3, 0.1, 0.1, 0.1, 0, null, true);
                shard.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.05, 0.05, 0.05, 0, null, true);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * Handle ice shard hitting something
     */
    @EventHandler
    public void onIceShardHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) return;
        if (!snowball.hasMetadata(META_ICE_SHARD)) return;

        UUID shooterUUID = null;
        if (snowball.hasMetadata(META_SHOOTER)) {
            shooterUUID = UUID.fromString(snowball.getMetadata(META_SHOOTER).get(0).asString());
        }

        Location hitLoc = snowball.getLocation();

        // Visual effects
        snowball.getWorld().spawnParticle(Particle.SNOWFLAKE, hitLoc, 20, 0.3, 0.3, 0.3, 0.1, null, true);
        snowball.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 10, 0.2, 0.2, 0.2, 0.05, null, true);
        snowball.getWorld().playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);

        // Damage and slow nearby entities
        Player shooter = shooterUUID != null ? plugin.getServer().getPlayer(shooterUUID) : null;

        for (LivingEntity entity : hitLoc.getNearbyLivingEntities(2.0)) {
            if (shooter != null && entity.equals(shooter)) continue;

            // Check trust
            if (shooter != null && entity instanceof Player targetPlayer) {
                if (plugin.getTrustManager().isTrusted(shooterUUID, targetPlayer.getUniqueId())) {
                    continue;
                }
            }

            // Damage (piercing - goes through armor)
            entity.damage(3.0, shooter);

            // Apply slowness
            entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, false, false)); // 3 seconds

            // Visual freeze effect
            entity.setFreezeTicks(entity.getMaxFreezeTicks() / 2);

            // Particle effect on hit
            entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0),
                    15, 0.3, 0.5, 0.3, 0, null, true);
        }

        // Cleanup
        iceShardProjectiles.remove(snowball.getUniqueId());
    }

    /**
     * Rotate a vector around the Y axis by the given angle
     */
    private Vector rotateAroundY(Vector vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double x = vector.getX() * cos + vector.getZ() * sin;
        double z = vector.getX() * -sin + vector.getZ() * cos;

        return new Vector(x, vector.getY(), z);
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Ice Shard Volley";
    }

    @Override
    public String getDescription() {
        return "Fire 5 ice shards in a cone that pierce enemies, dealing damage and applying slowness.";
    }
}