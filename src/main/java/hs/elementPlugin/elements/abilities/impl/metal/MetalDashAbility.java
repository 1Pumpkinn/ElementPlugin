package hs.elementPlugin.elements.abilities.impl.metal;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MetalDashAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public MetalDashAbility(ElementPlugin plugin) {
        super("metal_dash", 75, 15, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Vector direction = player.getLocation().getDirection().normalize();

        // Apply initial velocity boost
        Vector dashVelocity = direction.multiply(1.5);
        dashVelocity.setY(Math.max(dashVelocity.getY(), 0.3)); // Prevent downward dashing
        player.setVelocity(dashVelocity);

        // Track damaged entities to prevent multiple hits
        Set<UUID> damagedEntities = new HashSet<>();

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.5f);

        setActive(player, true);

        // Dash for 10 blocks (20 ticks at 0.5 blocks per tick)
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    setActive(player, false);
                    cancel();
                    return;
                }

                Location loc = player.getLocation();

                // Spawn metal particle trail
                player.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1, null, true);
                player.getWorld().spawnParticle(Particle.FIREWORK, loc, 5, 0.2, 0.2, 0.2, 0.05, null, true);

                // Check for nearby entities every 2 ticks
                if (ticks % 2 == 0) {
                    for (LivingEntity entity : loc.getNearbyLivingEntities(2.0)) {
                        if (entity.equals(player)) continue;
                        if (damagedEntities.contains(entity.getUniqueId())) continue;

                        // Check if valid target
                        if (entity instanceof Player targetPlayer) {
                            if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                continue;
                            }
                        }

                        // Damage the entity
                        entity.damage(4.0, player); // 2 hearts of damage
                        damagedEntities.add(entity.getUniqueId());

                        // Apply slight knockback
                        Vector knockback = entity.getLocation().toVector().subtract(loc.toVector()).normalize();
                        knockback.setY(0.3);
                        entity.setVelocity(knockback.multiply(0.5));

                        // Particle effect on hit
                        entity.getWorld().spawnParticle(Particle.CRIT, entity.getLocation(), 15, 0.5, 0.5, 0.5, 0.1, null, true);
                        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.2f);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.GRAY + "Metal Dash";
    }

    @Override
    public String getDescription() {
        return "Dash forward 10 blocks, damaging enemies you pass through. (75 mana)";
    }
}