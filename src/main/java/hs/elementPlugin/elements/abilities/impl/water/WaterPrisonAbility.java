package hs.elementPlugin.elements.abilities.impl.water;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

/**
 * Water Prison - Traps an enemy in a sphere of water, drowning them temporarily
 */
public class WaterPrisonAbility extends BaseAbility {
    private final ElementPlugin plugin;

    // Metadata key for tracking imprisoned entities
    public static final String META_WATER_PRISON = "water_prison_trapped";

    public WaterPrisonAbility(ElementPlugin plugin) {
        super("water_prison", 75, 15, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Raycast to find target
        RayTraceResult result = player.rayTraceEntities(20);
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage(ChatColor.RED + "No target found!");
            return false;
        }

        // Check if valid target
        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot target yourself!");
            return false;
        }

        // Don't target trusted players
        if (target instanceof Player targetPlayer) {
            if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot trap trusted players!");
                return false;
            }
        }

        // Set the target as active
        setActive(player, true);

        // Trap the target
        trapInWaterPrison(target, player);

        return true;
    }

    private void trapInWaterPrison(LivingEntity target, Player caster) {
        Location prisonCenter = target.getLocation().clone().add(0, 1, 0);

        // Play sound
        target.getWorld().playSound(prisonCenter, Sound.BLOCK_WATER_AMBIENT, 2.0f, 0.5f);
        target.getWorld().playSound(prisonCenter, Sound.ENTITY_PLAYER_SPLASH, 1.5f, 1.0f);

        // Duration: 6 seconds
        final int durationTicks = 120;
        final double radius = 2.0;

        // Mark entity as trapped
        long trapUntil = System.currentTimeMillis() + 6000L; // 6 seconds
        target.setMetadata(META_WATER_PRISON, new FixedMetadataValue(plugin, trapUntil));

        // Disable AI for mobs
        if (target instanceof Mob mob) {
            mob.setAware(false);
        }

        new BukkitRunnable() {
            int ticks = 0;
            final Location startLoc = target.getLocation().clone();

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || ticks >= durationTicks) {
                    setActive(caster, false);

                    // Remove metadata and restore AI
                    target.removeMetadata(META_WATER_PRISON, plugin);
                    if (target instanceof Mob mob) {
                        mob.setAware(true);
                    }

                    // Final particle burst when prison breaks
                    if (target.isValid()) {
                        target.getWorld().spawnParticle(
                                Particle.SPLASH,
                                target.getLocation().add(0, 1, 0),
                                100, 1.0, 1.0, 1.0, 0.3, null, true
                        );
                        target.getWorld().playSound(
                                target.getLocation(),
                                Sound.ENTITY_PLAYER_SPLASH,
                                2.0f, 1.5f
                        );
                    }

                    cancel();
                    return;
                }

                Location currentLoc = target.getLocation();

                // FIXED: Completely freeze entity in place
                // Cancel all velocity and teleport back to start if they moved
                target.setVelocity(new Vector(0, 0, 0));

                // If entity moved from starting position, teleport them back
                if (currentLoc.distance(startLoc) > 0.5) {
                    target.teleport(startLoc);
                }

                // Apply drowning effect
                if (ticks % 20 == 0) {
                    // Deal drowning damage
                    target.damage(1.0, caster);

                    // Apply slowness
                    target.addPotionEffect(
                            new PotionEffect(PotionEffectType.SLOWNESS, 25, 4, false, false, false)
                    );
                }

                // Reduce air supply
                if (target.getRemainingAir() > 0) {
                    target.setRemainingAir(Math.max(0, target.getRemainingAir() - 10));
                }

                // Create water sphere particles
                if (ticks % 2 == 0) {
                    createWaterSphere(startLoc.clone().add(0, 1, 0), radius);
                }

                // Add bubble particles inside
                if (ticks % 3 == 0) {
                    target.getWorld().spawnParticle(
                            Particle.BUBBLE_POP,
                            startLoc.clone().add(0, 1, 0),
                            20, 0.5, 0.5, 0.5, 0.1, null, true
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void createWaterSphere(Location center, double radius) {
        World world = center.getWorld();

        // Create sphere using spherical coordinates
        int points = 30;
        for (int i = 0; i < points; i++) {
            double theta = Math.PI * i / points; // 0 to π
            for (int j = 0; j < points; j++) {
                double phi = 2 * Math.PI * j / points; // 0 to 2π

                double x = radius * Math.sin(theta) * Math.cos(phi);
                double y = radius * Math.cos(theta);
                double z = radius * Math.sin(theta) * Math.sin(phi);

                Location particleLoc = center.clone().add(x, y, z);

                world.spawnParticle(
                        Particle.DRIPPING_WATER,
                        particleLoc,
                        1, 0.0, 0.0, 0.0, 0.0, null, true
                );

                // Add some falling water particles
                if (Math.random() < 0.3) {
                    world.spawnParticle(
                            Particle.FALLING_WATER,
                            particleLoc,
                            1, 0.0, 0.0, 0.0, 0.0, null, true
                    );
                }
            }
        }
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Water Prison";
    }

    @Override
    public String getDescription() {
        return "Trap an enemy in a sphere of water, drowning them for 6 seconds.";
    }
}