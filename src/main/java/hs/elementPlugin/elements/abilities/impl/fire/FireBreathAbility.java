package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import com.destroystokyo.paper.ParticleBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FireBreathAbility extends BaseAbility {
    private final Set<UUID> activeUsers = new HashSet<>();
    private final hs.elementPlugin.ElementPlugin plugin;
    
    public FireBreathAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("fire_breath", 50, 10, 1);
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        
        // Clear any existing active state first to prevent glitches
        setActive(player, false);
        
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        setActive(player, true);
        
        player.sendMessage(ChatColor.GOLD + "Fire Breath activated!");

        new BukkitRunnable() {
            private int ticks = 0;
            private final int maxTicks = 100; // 5 seconds
            private boolean isCancelled = false;

            @Override
            public void run() {
                if (isCancelled || !player.isOnline() || ticks >= maxTicks) {
                    setActive(player, false);
                    player.sendMessage(ChatColor.GOLD + "Fire Breath deactivated!");
                    cancel();
                    return;
                }

// --- Smooth performance-friendly cone fire effect ---
                Location eyeLoc = player.getEyeLocation();
                Vector direction = eyeLoc.getDirection().normalize();

                double maxDistance = 6.0;
                double startDistance = 0.5;

// cone angle (narrower = more focused)
                double coneAngle = Math.toRadians(25);
                double cosThreshold = Math.cos(coneAngle); // ADD THIS LINE - for fast entity detection

// Get perpendicular vectors for creating the cone cross-sections
                Vector perp1 = getPerpendicular(direction);
                Vector perp2 = direction.clone().crossProduct(perp1).normalize();

// Spacing between distance steps
                double distanceStep = 0.4;

                for (double distance = startDistance; distance <= maxDistance; distance += distanceStep) {
                    Location particleBase = eyeLoc.clone().add(direction.clone().multiply(distance));

                    // Stop if we hit a solid block
                    if (!isPassableBlock(particleBase.getBlock())) break;

                    // CONE MATH
                    // For a true cone: radius = distance × tan(angle)
                    // At distance d from apex (eye), the cone radius is: r = d × tan(coneAngle)
                    double radius = distance * Math.tan(coneAngle);

                    // PARTICLE DENSITY
                    // To maintain visual consistency, we need MORE particles as radius grows
                    // Circumference = 2πr, so particles needed scales with radius
                    // However, for performance, we use a compromise: sqrt scaling
                    // This gives perception of fullness without linear particle growth
                    int particles = Math.max(4, (int) Math.ceil(6 + radius * 3));

                    // Clamp maximum particles per ring for performance
                    particles = Math.min(particles, 16);

                    for (int i = 0; i < particles; i++) {
                        // Evenly distribute particles around the circle
                        double angle = (2 * Math.PI * i) / particles;

                        // Create offset vector in the perpendicular plane
                        // This forms a circle of radius r perpendicular to the direction
                        Vector offset = perp1.clone().multiply(radius * Math.cos(angle))
                                .add(perp2.clone().multiply(radius * Math.sin(angle)));

                        // Add subtle turbulence for fire flicker effect
                        // Keep it small to maintain cone shape visibility
                        offset.add(new Vector(
                                (Math.random() - 0.5) * 0.08,
                                (Math.random() - 0.5) * 0.08,
                                (Math.random() - 0.5) * 0.08
                        ));

                        Location finalLoc = particleBase.clone().add(offset);

                        // Alternate between FLAME and SMALL_FLAME for variety
                        Particle particleType = (Math.random() < 0.6) ? Particle.SMALL_FLAME : Particle.FLAME;

                        new ParticleBuilder(particleType)
                                .location(finalLoc)
                                .count(1)
                                .offset(0, 0, 0)
                                .extra(0)
                                .receivers(player)
                                .spawn();
                    }
                }

// Entity detection using dot >= cosThreshold (faster than using acos)
                for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                    if (entity == player) continue;

                    Location entityLoc = entity.getEyeLocation();
                    Vector toEntity = entityLoc.toVector().subtract(eyeLoc.toVector());
                    double distanceToEntity = toEntity.length();
                    if (distanceToEntity < startDistance || distanceToEntity > maxDistance) continue;

                    toEntity.normalize();
                    double dot = toEntity.dot(direction);

                    if (dot >= cosThreshold) { // inside cone
                        entity.setFireTicks(Math.max(entity.getFireTicks(), 60)); // 3s
                        if (ticks % 10 == 0) { // periodic damage
                            entity.damage(2.0, player);
                        }
                    }
                }

                // Check for entities in the cone and set them on fire
                for (LivingEntity entity : player.getWorld().getLivingEntities()) {
                    if (entity == player) continue;

                    Location entityLoc = entity.getEyeLocation();
                    Vector toEntity = entityLoc.toVector().subtract(eyeLoc.toVector());
                    double distanceToEntity = toEntity.length();

                    if (distanceToEntity > maxDistance || distanceToEntity < startDistance) continue;

                    toEntity.normalize();
                    double dot = toEntity.dot(direction);
                    double angle = Math.acos(Math.max(-1, Math.min(1, dot))); // Clamp to prevent NaN

                    if (angle <= coneAngle) {
                        entity.setFireTicks(Math.max(entity.getFireTicks(), 60)); // 3 seconds of fire

                        // Apply damage every 10 ticks (0.5 seconds)
                        if (ticks % 10 == 0) {
                            entity.damage(2.0, player); // 1 heart of damage
                        }
                    }
                }

                ticks++;
            }
            
            @Override
            public void cancel() {
                super.cancel();
                isCancelled = true;
            }
        }.runTaskTimer(context.getPlugin(), 0L, 1L);
        
        return true;
    }
    
    // Using the base class implementation for isActiveFor and setActive
    
    public void clearEffects(Player player) {
        setActive(player, false);
    }
    
    @Override
    public String getName() {
        return ChatColor.RED + "Fire Breath";
    }
    
    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Breathe a cone of fire that ignites enemies. (50 mana)";
    }
    
    // Helper method to get a perpendicular vector
    private Vector getPerpendicular(Vector vector) {
        if (vector.getX() == 0 && vector.getZ() == 0) {
            return new Vector(1, 0, 0);
        } else {
            return new Vector(-vector.getZ(), 0, vector.getX()).normalize();
        }
    }
    
    // Helper method to check if a block should be passable for the fire breath
    private boolean isPassableBlock(Block block) {
        if (block == null) return true;
        
        Material type = block.getType();
        
        // Allow passage through air and void
        if (type == Material.AIR || type == Material.VOID_AIR || type == Material.CAVE_AIR) {
            return true;
        }
        
        // Allow passage through small plants, flowers, and decorative blocks
        return type == Material.TALL_GRASS ||
                type == Material.GRASS_BLOCK ||
                type == Material.SHORT_GRASS ||
                type == Material.FERN ||
               type == Material.LARGE_FERN ||
               type == Material.DEAD_BUSH ||
               type == Material.DANDELION ||
               type == Material.POPPY ||
               type == Material.BLUE_ORCHID ||
               type == Material.ALLIUM ||
               type == Material.AZURE_BLUET ||
               type == Material.RED_TULIP ||
               type == Material.ORANGE_TULIP ||
               type == Material.WHITE_TULIP ||
               type == Material.PINK_TULIP ||
               type == Material.OXEYE_DAISY ||
               type == Material.CORNFLOWER ||
               type == Material.LILY_OF_THE_VALLEY ||
               type == Material.SUNFLOWER ||
               type == Material.LILAC ||
               type == Material.ROSE_BUSH ||
               type == Material.PEONY ||
               type == Material.SWEET_BERRY_BUSH ||
               type == Material.BAMBOO ||
               type == Material.SUGAR_CANE ||
               type == Material.KELP ||
               type == Material.SEAGRASS ||
               type == Material.TALL_SEAGRASS ||
               type == Material.WHEAT ||
               type == Material.CARROTS ||
               type == Material.POTATOES ||
               type == Material.BEETROOTS ||
               type == Material.MELON_STEM ||
               type == Material.PUMPKIN_STEM ||
               type == Material.TORCH ||
               type == Material.REDSTONE_TORCH ||
               type == Material.SOUL_TORCH ||
               type == Material.REDSTONE_WIRE ||
               type == Material.TRIPWIRE ||
               type == Material.TRIPWIRE_HOOK ||
               type == Material.LEVER ||
               type == Material.STONE_BUTTON ||
               type == Material.OAK_BUTTON ||
               type == Material.SPRUCE_BUTTON ||
               type == Material.BIRCH_BUTTON ||
               type == Material.JUNGLE_BUTTON ||
               type == Material.ACACIA_BUTTON ||
               type == Material.DARK_OAK_BUTTON ||
               type == Material.CRIMSON_BUTTON ||
               type == Material.WARPED_BUTTON ||
               type == Material.POLISHED_BLACKSTONE_BUTTON ||
               type == Material.LIGHT_WEIGHTED_PRESSURE_PLATE ||
               type == Material.HEAVY_WEIGHTED_PRESSURE_PLATE ||
               type == Material.SPRUCE_PRESSURE_PLATE ||
               type == Material.BIRCH_PRESSURE_PLATE ||
               type == Material.JUNGLE_PRESSURE_PLATE ||
               type == Material.ACACIA_PRESSURE_PLATE ||
               type == Material.DARK_OAK_PRESSURE_PLATE ||
               type == Material.CRIMSON_PRESSURE_PLATE ||
               type == Material.WARPED_PRESSURE_PLATE ||
               type == Material.POLISHED_BLACKSTONE_PRESSURE_PLATE ||
               type == Material.STONE_PRESSURE_PLATE ||
               type == Material.RAIL ||
               type == Material.POWERED_RAIL ||
               type == Material.DETECTOR_RAIL ||
               type == Material.ACTIVATOR_RAIL ||
               type == Material.COBWEB ||
               type == Material.VINE ||
               type == Material.LADDER ||
               type == Material.SCAFFOLDING ||
               type == Material.SNOW ||
               type == Material.WATER ||
               type == Material.LAVA ||
               type == Material.FIRE ||
               type == Material.SOUL_FIRE;
    }
}