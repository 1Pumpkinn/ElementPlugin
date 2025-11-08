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
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FrostCircleAbility extends BaseAbility {
    private final ElementPlugin plugin;
    private final Set<UUID> activeCircles = new HashSet<>();

    public static final String META_CIRCLE_FROZEN = "frost_freezing_circle";

    public FrostCircleAbility(ElementPlugin plugin) {
        super("frost_freezing_circle", 50, 10, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Check if already active
        if (activeCircles.contains(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Freezing Circle is already active!");
            return false;
        }

        setActive(player, true);
        activeCircles.add(player.getUniqueId());

        // Play activation sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

        final Location centerLocation = player.getLocation().clone();
        final double radius = 5.0;

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 200; // 10 seconds

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    // Cleanup: Remove all frozen metadata
                    for (LivingEntity entity : centerLocation.getNearbyLivingEntities(radius + 2)) {
                        if (entity.hasMetadata(META_CIRCLE_FROZEN)) {
                            entity.removeMetadata(META_CIRCLE_FROZEN, plugin);
                            // Remove slowness effect
                            entity.removePotionEffect(PotionEffectType.SLOWNESS);
                            if (entity instanceof Mob mob) {
                                mob.setAware(true);
                            }
                        }
                    }

                    setActive(player, false);
                    activeCircles.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                // Create ice particle circle at ground level
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;

                    Location particleLoc = centerLocation.clone().add(x, 0.1, z);

                    // Ensure particles are visible above ground
                    while (particleLoc.getBlock().getType().isSolid() && particleLoc.getY() < centerLocation.getY() + 3) {
                        particleLoc.add(0, 1, 0);
                    }

                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 1, 0.1, 0.1, 0.1, 0, null, true);

                    // Add some frost effect
                    if (i % 30 == 0) {
                        player.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 2, 0.2, 0.2, 0.2, 0, null, true);
                    }
                }

                // Check for enemies in circle every tick and apply freeze
                for (LivingEntity entity : centerLocation.getNearbyLivingEntities(radius)) {
                    if (entity.equals(player)) continue;

                    // Check if entity is a player and if they're trusted - skip if trusted
                    if (entity instanceof Player targetPlayer) {
                        if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                            continue;
                        }
                    }

                    // Apply freezing effect (same as powder snow)
                    entity.setFreezeTicks(entity.getMaxFreezeTicks());

                    // Mark entity as frozen (for slow movement instead of complete freeze)
                    if (!entity.hasMetadata(META_CIRCLE_FROZEN)) {
                        entity.setMetadata(META_CIRCLE_FROZEN, new FixedMetadataValue(plugin, true));
                    }

                    // Apply Slowness 4 (very slow movement instead of no movement)
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 3, false, false, false));

                    // For mobs, disable AI while in circle
                    if (entity instanceof Mob mob) {
                        mob.setAware(true);
                    }

                    // Visual feedback every 10 ticks to reduce particle spam
                    if (ticks % 10 == 0) {
                        entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0, null, true);
                    }
                }

                // Re-enable AI for mobs that left the circle and remove effects
                for (LivingEntity entity : player.getWorld().getNearbyLivingEntities(centerLocation, radius + 2)) {
                    if (entity.hasMetadata(META_CIRCLE_FROZEN)) {
                        double distance = entity.getLocation().distance(centerLocation);
                        if (distance > radius) {
                            entity.removeMetadata(META_CIRCLE_FROZEN, plugin);
                            entity.removePotionEffect(PotionEffectType.SLOWNESS);
                            if (entity instanceof Mob mob) {
                                mob.setAware(true);
                            }
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Freezing Circle";
    }

    @Override
    public String getDescription() {
        return "Create a circle around you that severely slows enemies who step inside for 10 seconds.";
    }

    @Override
    protected boolean isValidTarget(ElementContext context, LivingEntity entity) {
        if (entity.equals(context.getPlayer())) return false;

        if (entity instanceof Player targetPlayer) {
            return !context.getTrustManager().isTrusted(context.getPlayer().getUniqueId(), targetPlayer.getUniqueId());
        }

        return true;
    }
}