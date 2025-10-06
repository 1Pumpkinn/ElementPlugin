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
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();

        if (!context.getManaManager().hasMana(player, 50)) {
            player.sendMessage(ChatColor.RED + "Not enough mana! (50 required)");
            return false;
        }

        context.getManaManager().spend(player, 50);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        setAbility1Active(player, true);

        final java.util.Set<LivingEntity> affectedEntities = new java.util.HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 100) {
                    setAbility1Active(player, false);
                    cancel();
                    return;
                }

                Location playerLoc = player.getLocation().add(0, 1.2, 0);
                Vector dir = player.getLocation().getDirection().normalize();

                Vector startOffset = dir.clone().multiply(1.5);
                Location startLoc = playerLoc.clone().add(startOffset);

                // Get perpendicular vectors for creating the cone
                Vector perpX = getPerpendicular(dir);
                Vector perpY = dir.clone().crossProduct(perpX).normalize();

                // Create cone shape that widens as it goes out
                for (double d = 0; d <= 6; d += 0.5) {
                    Vector step = dir.clone().multiply(d);
                    Location centerLoc = startLoc.clone().add(step);

                    double maxRadius = d * 0.25;

                    // Create filled cone by spawning particles in concentric circles
                    int radiusSteps = Math.max(1, (int)(maxRadius / 0.3));
                    for (int r = 0; r <= radiusSteps; r++) {
                        double radius = (maxRadius * r) / radiusSteps;
                        int particleCount = Math.max(3, (int)(radius * 8));

                        for (int i = 0; i < particleCount; i++) {
                            double angle = (Math.PI * 2 * i) / particleCount;
                            Vector offset = perpX.clone().multiply(Math.cos(angle) * radius)
                                    .add(perpY.clone().multiply(Math.sin(angle) * radius));
                            Location particleLoc = centerLoc.clone().add(offset);
                            player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.03, 0.03, 0.03, 0.01);
                        }
                    }

                    for (LivingEntity le : centerLoc.getNearbyLivingEntities(maxRadius + 0.5)) {
                        if (!isValidTarget(context, le)) continue;
                        le.setFireTicks(100);
                        affectedEntities.add(le);
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

                if (ticks % 20 == 0) {
                    for (LivingEntity entity : affectedEntities) {
                        if (entity.isValid() && !entity.isDead()) {
                            double currentHealth = entity.getHealth();
                            double newHealth = Math.max(0, currentHealth - 1.0);
                            entity.setHealth(newHealth);
                        }
                    }
                }

                if (ticks >= 100) {
                    cancel();
                    affectedEntities.clear();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }

    private Vector getPerpendicular(Vector v) {
        if (Math.abs(v.getX()) < 0.9) {
            return new Vector(1, 0, 0).crossProduct(v).normalize();
        } else {
            return new Vector(0, 1, 0).crossProduct(v).normalize();
        }
    }

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

        if (!context.getManaManager().hasMana(player, 75)) {
            player.sendMessage(ChatColor.RED + "Not enough mana! (75 required)");
            return false;
        }

        context.getManaManager().spend(player, 75);
        setAbility2Active(player, true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);

        final java.util.Set<LivingEntity> affectedEntities = new java.util.HashSet<>();

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 200) {
                    setAbility2Active(player, false);
                    cancel();
                    return;
                }

                Location eyeLocation = player.getEyeLocation();
                Vector direction = player.getLocation().getDirection().normalize();

                for (double d = 1.0; d <= 15.0; d += 0.5) {
                    Vector pos = direction.clone().multiply(d);
                    Location particleLoc = eyeLocation.clone().add(pos);
                    player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);

                    for (LivingEntity entity : particleLoc.getNearbyLivingEntities(1.0)) {
                        if (!isValidTarget(context, entity)) continue;

                        Vector knockback = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                        knockback.setY(0.2);
                        knockback = knockback.multiply(0.3);
                        entity.setVelocity(knockback);
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

                if (ticks % 20 == 0) {
                    for (LivingEntity entity : affectedEntities) {
                        if (entity.isValid() && !entity.isDead()) {
                            double currentHealth = entity.getHealth();
                            double newHealth = Math.max(0, currentHealth - 0.5);
                            entity.setHealth(newHealth);
                            entity.getWorld().spawnParticle(Particle.FLAME, entity.getLocation().add(0, 1, 0), 5, 0.2, 0.2, 0.2, 0.05);
                        }
                    }
                }

                if (ticks >= 200) {
                    cancel();
                    affectedEntities.clear();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);

        return true;
    }
}