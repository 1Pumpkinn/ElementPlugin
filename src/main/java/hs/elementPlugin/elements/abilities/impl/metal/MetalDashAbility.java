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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MetalDashAbility extends BaseAbility implements Listener {
    private final ElementPlugin plugin;
    private final Set<UUID> stunnedPlayers = new HashSet<>();
    private final Set<UUID> dashingPlayers = new HashSet<>();
    private final Map<UUID, Boolean> pendingStuns = new HashMap<>();

    public MetalDashAbility(ElementPlugin plugin) {
        super("metal_dash", 75, 15, 2);
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Vector direction = player.getLocation().getDirection().normalize();

        // Store the initial dash direction
        final Vector dashDirection = direction.clone();

        // Apply stronger initial velocity boost for longer dash
        Vector dashVelocity = direction.multiply(2.5);
        dashVelocity.setY(Math.max(dashVelocity.getY(), 0.4)); // Prevent downward dashing
        player.setVelocity(dashVelocity);

        // Track damaged entities to prevent multiple hits
        Set<UUID> damagedEntities = new HashSet<>();

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 1.5f);

        setActive(player, true);
        dashingPlayers.add(player.getUniqueId());

        // Dash for 20 blocks (40 ticks at 0.5 blocks per tick)
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40;
            boolean hitEntity = false;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    setActive(player, false);
                    dashingPlayers.remove(player.getUniqueId());

                    // Apply stun if no entity was hit
                    if (!hitEntity) {
                        // Check if player is on ground, if not, mark for pending stun
                        if (player.isOnGround()) {
                            applyStun(player);
                        } else {
                            pendingStuns.put(player.getUniqueId(), true);
                        }
                    }

                    cancel();
                    return;
                }

                Location loc = player.getLocation();

                // Spawn metal particle trail
                player.getWorld().spawnParticle(Particle.CRIT, loc, 10, 0.3, 0.3, 0.3, 0.1, null, true);
                player.getWorld().spawnParticle(Particle.FIREWORK, loc, 5, 0.2, 0.2, 0.2, 0.05, null, true);

                // Check for nearby entities every 2 ticks
                if (ticks % 2 == 0) {
                    for (LivingEntity entity : loc.getNearbyLivingEntities(2.5)) {
                        if (entity.equals(player)) continue;
                        if (damagedEntities.contains(entity.getUniqueId())) continue;

                        // Skip armor stands
                        if (entity instanceof org.bukkit.entity.ArmorStand) continue;

                        // Check if valid target
                        if (entity instanceof Player targetPlayer) {
                            if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                continue;
                            }
                        }

                        // Deal TRUE DAMAGE (ignore armor/resistance)
                        double currentHealth = entity.getHealth();
                        double newHealth = Math.max(0, currentHealth - 4.0);
                        entity.setHealth(newHealth);


                        // Mark as damaged
                        damagedEntities.add(entity.getUniqueId());
                        hitEntity = true;

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

    private void applyStun(Player player) {
        // Add player to stunned set
        stunnedPlayers.add(player.getUniqueId());
        pendingStuns.remove(player.getUniqueId());

        // Visual and audio feedback for the stun
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.05, null, true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_DAMAGE, 1.0f, 0.8f);


        // Remove from stunned set after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                stunnedPlayers.remove(player.getUniqueId());
            }
        }.runTaskLater(plugin, 100L);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        // Check for pending stun when player lands
        if (pendingStuns.containsKey(playerId) && player.isOnGround()) {
            applyStun(player);
        }

        // Check if player is stunned
        if (stunnedPlayers.contains(playerId)) {
            Location from = event.getFrom();
            Location to = event.getTo();

            // If player tried to move (not just looking around)
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                // Cancel the movement by teleporting back
                event.setTo(from);

                // Optional: Small visual feedback
                if (player.getTicksLived() % 10 == 0) { // Only every 10 ticks to avoid spam
                }
            }
        }
    }

    @Override
    public String getName() {
        return ChatColor.GRAY + "Metal Dash";
    }

    @Override
    public String getDescription() {
        return "Dash forward 20 blocks, damaging enemies you pass through. Missing all enemies stuns you for 5 seconds. (75 mana)";
    }
}