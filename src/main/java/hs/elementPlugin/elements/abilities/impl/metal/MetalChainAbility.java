package hs.elementPlugin.elements.abilities.impl.metal;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MetalChainAbility extends BaseAbility {
    private final ElementPlugin plugin;

    // Metadata key for stun tracking
    public static final String META_CHAINED_STUN = "metal_chain_stunned";

    public MetalChainAbility(ElementPlugin plugin) {
        super("metal_chain", 75, 10, 2); // Now ability 2: 75 mana, level 2 required
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // --- Target detection (ray trace for precise targeting) ---
        double range = 12.0;

        // First, check if there's a block in the way
        org.bukkit.util.RayTraceResult blockRayTrace = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                range,
                org.bukkit.FluidCollisionMode.NEVER,
                true
        );

        // If a block is hit, limit the range to that distance
        double effectiveRange = range;
        if (blockRayTrace != null && blockRayTrace.getHitBlock() != null) {
            effectiveRange = player.getEyeLocation().distance(blockRayTrace.getHitPosition().toLocation(player.getWorld()));
        }

        // Now ray trace for entities, but only up to the block distance
        org.bukkit.util.RayTraceResult rayTrace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                effectiveRange,
                0.5, // Hit box expansion
                entity -> {
                    // Filter out invalid targets
                    if (entity.equals(player)) return false;
                    if (entity instanceof org.bukkit.entity.ArmorStand) return false;
                    if (!(entity instanceof LivingEntity)) return false;

                    // Check trust for players
                    if (entity instanceof Player targetPlayer) {
                        return !context.getTrustManager().isTrusted(
                                player.getUniqueId(),
                                targetPlayer.getUniqueId()
                        );
                    }

                    return true;
                }
        );

        if (rayTrace == null || rayTrace.getHitEntity() == null) {
            player.sendMessage(ChatColor.RED + "No target found! Aim at an enemy.");
            return false;
        }

        LivingEntity target = (LivingEntity) rayTrace.getHitEntity();

        // Don't target trusted players (redundant check but kept for safety)
        if (target instanceof Player targetPlayer) {
            if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot chain trusted players!");
                return false;
            }
        }

        // Play sounds
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 1.0f);

        final LivingEntity finalTarget = target;

        // Start chain particle animation and reeling
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40; // 2 seconds of reeling

            @Override
            public void run() {
                if (!player.isOnline() || !finalTarget.isValid() || ticks >= maxTicks) {
                    cancel();
                    return;
                }

                // Get current positions
                Location currentPlayerLoc = player.getEyeLocation().subtract(0, 0.3, 0);
                Location targetLoc = finalTarget.getEyeLocation();

                // Calculate distance
                double distance = currentPlayerLoc.distance(targetLoc);

                // If target is close enough, stop
                if (distance < 2.0) {
                    // Set velocity to zero to stop movement
                    finalTarget.setVelocity(new Vector(0, 0, 0));

                    if (finalTarget instanceof Mob mob) {
                        mob.setAware(true);
                    }

                    // Stun the target for 3 seconds
                    stunEntity(finalTarget, 3);

                    // Visual/audio feedback for stun
                    player.getWorld().playSound(targetLoc, Sound.BLOCK_ANVIL_LAND, 1.0f, 2.0f);
                    player.getWorld().spawnParticle(Particle.BLOCK, targetLoc, 30,
                            0.3, 0.5, 0.3, 0.1,
                            org.bukkit.Material.IRON_BLOCK.createBlockData(), true);

                    cancel();
                    return;
                }

                // Draw particle chain
                Vector direction = targetLoc.toVector().subtract(currentPlayerLoc.toVector()).normalize();
                double particleSpacing = 0.3;
                int numParticles = (int) (distance / particleSpacing);

                for (int i = 0; i <= numParticles; i++) {
                    double t = i * particleSpacing;
                    Location particleLoc = currentPlayerLoc.clone().add(direction.clone().multiply(t));

                    // Main chain particles (iron blocks/anvil look)
                    player.getWorld().spawnParticle(Particle.BLOCK, particleLoc, 1,
                            0.05, 0.05, 0.05, 0.0,
                            org.bukkit.Material.IRON_BLOCK.createBlockData(), true);

                    // Add some sparkles for effect
                    if (i % 3 == 0) {
                        player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1,
                                0.02, 0.02, 0.02, 0.01, null, true);
                    }
                }

                // Apply gentle pull force to target
                Vector pullDirection = currentPlayerLoc.toVector().subtract(targetLoc.toVector()).normalize();

                // Gentle upward component to prevent dragging on ground
                pullDirection.setY(pullDirection.getY() + 0.1);

                // Smooth pull force (not instant velocity)
                double pullStrength = 0.25; // Gentle pull
                Vector currentVelocity = finalTarget.getVelocity();
                Vector newVelocity = currentVelocity.add(pullDirection.multiply(pullStrength));

                // Cap velocity to prevent launching
                double maxSpeed = 0.8;
                if (newVelocity.length() > maxSpeed) {
                    newVelocity = newVelocity.normalize().multiply(maxSpeed);
                }

                finalTarget.setVelocity(newVelocity);

                // Play chain sound periodically
                if (ticks % 10 == 0) {
                    player.getWorld().playSound(currentPlayerLoc, Sound.BLOCK_CHAIN_STEP, 0.5f, 1.2f);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    /**
     * Stun an entity for a duration
     */
    private void stunEntity(LivingEntity entity, int durationSeconds) {
        long stunUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        entity.setMetadata(META_CHAINED_STUN, new FixedMetadataValue(plugin, stunUntil));

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

        // Visual stun effect - periodic particles + gravity handling
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (!entity.isValid() || entity.isDead() || ticks >= maxTicks) {
                    entity.removeMetadata(META_CHAINED_STUN, plugin);
                    cancel();
                    return;
                }

                if (!entity.hasMetadata(META_CHAINED_STUN)) {
                    cancel();
                    return;
                }

                long end = entity.getMetadata(META_CHAINED_STUN).get(0).asLong();
                if (System.currentTimeMillis() >= end) {
                    entity.removeMetadata(META_CHAINED_STUN, plugin);
                    cancel();
                    return;
                }

                // CRITICAL FIX: Apply gravity if there's a hole beneath entity
                if (!entity.isOnGround()) {
                    // Check if there's air/void beneath (entity should fall)
                    Location checkLoc = entity.getLocation().subtract(0, 1, 0);
                    if (checkLoc.getBlock().getType().isAir() || !checkLoc.getBlock().getType().isSolid()) {
                        // Apply downward velocity to make them fall
                        Vector gravity = new Vector(0, -0.5, 0);
                        entity.setVelocity(gravity);
                    }
                } else {
                    // Entity is on ground - keep them in place
                    entity.setVelocity(new Vector(0, 0, 0));
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
        return ChatColor.GRAY + "Chain Reel";
    }

    @Override
    public String getDescription() {
        return "Pull a targeted enemy towards you with a chain. (75 mana)";
    }
}