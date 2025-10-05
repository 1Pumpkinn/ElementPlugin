package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireElement extends BaseElement {
    public static final String META_FRIENDLY_BLAZE_OWNER = "fire_friendly_owner";

    public FireElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.FIRE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Infinite Fire Resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
        // Upside 2 is handled elsewhere (auto-smelt)
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();

        // Check for 50 mana cost
        if (!context.getManaManager().hasMana(player, 50)) {
            player.sendMessage(ChatColor.RED + "Not enough mana! (50 required)");
            return false;
        }

        // Consume mana
        context.getManaManager().spend(player, 50);

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);

        // Mark ability as active to prevent stacking
        setAbility1Active(player, true);

        // Store affected entities to apply DoT effect
        final java.util.Set<LivingEntity> affectedEntities = new java.util.HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 100) { // 5 seconds (100 ticks)
                    setAbility1Active(player, false);
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation().add(0, 1.2, 0); // Chest level
                Vector dir = player.getLocation().getDirection().normalize();

                // Start cone 1.5 blocks in front of player at chest level
                Vector startOffset = dir.clone().multiply(1.5);
                Location startLoc = playerLoc.clone().add(startOffset);

                // Get perpendicular vectors for creating the cone
                Vector perpX = getPerpendicular(dir);
                Vector perpY = dir.clone().crossProduct(perpX).normalize();

                // Create cone shape that widens as it goes out
                for (double d = 0; d <= 6; d += 0.5) {
                    Vector step = dir.clone().multiply(d);
                    Location centerLoc = startLoc.clone().add(step);

                    // Cone radius increases with distance
                    double maxRadius = d * 0.25; // Starts at 0, ends at 1.5 blocks wide

                    // Create filled cone by spawning particles in concentric circles
                    int radiusSteps = Math.max(1, (int)(maxRadius / 0.3));
                    for (int r = 0; r <= radiusSteps; r++) {
                        double radius = (maxRadius * r) / radiusSteps;

                        // Number of particles around this circle
                        int particleCount = Math.max(3, (int)(radius * 8));

                        for (int i = 0; i < particleCount; i++) {
                            double angle = (Math.PI * 2 * i) / particleCount;

                            // Calculate position using perpendicular vectors
                            Vector offset = perpX.clone().multiply(Math.cos(angle) * radius)
                                    .add(perpY.clone().multiply(Math.sin(angle) * radius));

                            Location particleLoc = centerLoc.clone().add(offset);

                            // Spawn flame particles
                            player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.03, 0.03, 0.03, 0.01);
                        }
                    }

                    // Damage entities in cone (no knockback)
                    for (LivingEntity le : centerLoc.getNearbyLivingEntities(maxRadius + 0.5)) {
                        if (!isValidTarget(context, le)) continue;
                        le.setFireTicks(100); // 5 seconds of fire

                        // Knockback removed as requested

                        // Add to affected entities for DoT
                        affectedEntities.add(le);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for continuous fire breath

        // Apply damage over time to affected entities
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                // Apply damage every second (20 ticks)
                if (ticks % 20 == 0) {
                    for (LivingEntity entity : affectedEntities) {
                        if (entity.isValid() && !entity.isDead()) {
                            // Apply half a heart damage (1 point)
                            double currentHealth = entity.getHealth();
                            double newHealth = Math.max(0, currentHealth - 1.0);
                            entity.setHealth(newHealth);
                        }
                    }
                }

                // End after 5 seconds (100 ticks)
                if (ticks >= 100) {
                    cancel();
                    affectedEntities.clear();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    // Helper method to get a perpendicular vector
    private Vector getPerpendicular(Vector v) {
        if (Math.abs(v.getX()) < 0.9) {
            return new Vector(1, 0, 0).crossProduct(v).normalize();
        } else {
            return new Vector(0, 1, 0).crossProduct(v).normalize();
        }
    }

    // Helper method to rotate a vector around an axis
    private Vector rotateAroundAxis(Vector v, Vector axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = v.dot(axis);

        return v.clone().multiply(cos)
                .add(axis.clone().crossProduct(v).multiply(sin))
                .add(axis.clone().multiply(dot * (1 - cos)));
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();

        // Check for 75 mana cost
        if (!context.getManaManager().hasMana(player, 75)) {
            player.sendMessage(ChatColor.RED + "Not enough mana! (75 required)");
            return false;
        }

        // Consume mana
        context.getManaManager().spend(player, 75);

        // Mark ability as active to prevent stacking
        setAbility2Active(player, true);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);

        // Store affected entities to apply DoT effect
        final java.util.Set<LivingEntity> affectedEntities = new java.util.HashSet<>();

        // Create fire beam that extends from player's face
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 200) { // 10 seconds (200 ticks)
                    setAbility2Active(player, false);
                    cancel();
                    return;
                }

                Location eyeLocation = player.getEyeLocation();
                Vector direction = player.getLocation().getDirection().normalize();

                // Create beam particles
                for (double d = 1.0; d <= 15.0; d += 0.5) {
                    Vector pos = direction.clone().multiply(d);
                    Location particleLoc = eyeLocation.clone().add(pos);

                    // Spawn fewer particles for better visibility
                    player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);

                    // Check for entities in beam path
                    for (LivingEntity entity : particleLoc.getNearbyLivingEntities(1.0)) {
                        if (!isValidTarget(context, entity)) continue;

                        // Apply knockback
                        Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                        knockback.setY(0.2); // Add slight upward component
                        knockback = knockback.multiply(0.3); // Light knockback
                        entity.setVelocity(knockback);

                        // Add to affected entities for DoT
                        affectedEntities.add(entity);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Apply damage over time to affected entities
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                ticks++;

                // Apply damage every second (20 ticks)
                if (ticks % 20 == 0) {
                    for (LivingEntity entity : affectedEntities) {
                        if (entity.isValid() && !entity.isDead()) {
                            // Apply quarter heart damage (0.5 point)
                            double currentHealth = entity.getHealth();
                            double newHealth = Math.max(0, currentHealth - 0.5);
                            entity.setHealth(newHealth);

                            // Visual effect for damage
                            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                }

                // End after 10 seconds (200 ticks)
                if (ticks >= 200) {
                    cancel();
                    affectedEntities.clear();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }
}