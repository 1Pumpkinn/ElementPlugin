package hs.elementPlugin.elements.abilities.impl.frost;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class FrostNovaAbility extends BaseAbility {
    private final ElementPlugin plugin;
    private final Set<Location> frozenBlocks = new HashSet<>();

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

        // Play explosion sound
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 2.0f, 0.5f);
        player.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.5f);

        // Create expanding ice explosion effect
        new BukkitRunnable() {
            double currentRadius = 0;
            int ticks = 0;

            @Override
            public void run() {
                if (currentRadius >= radius || ticks >= 15) {
                    cancel();
                    return;
                }

                // Spawn particle ring
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    Location particleLoc = center.clone().add(x, 0.1, z);

                    // Ice particles
                    player.getWorld().spawnParticle(Particle.SNOWFLAKE, particleLoc, 3, 0.1, 0.2, 0.1, 0, null, true);
                    player.getWorld().spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0, null, true);
                }

                currentRadius += 0.5;
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Freeze ground in radius
        freezeGround(center, radius);

        // Freeze nearby enemies
        for (LivingEntity entity : center.getNearbyLivingEntities(radius)) {
            if (entity.equals(player)) continue;

            // Check if entity is a player and if they're trusted
            if (entity instanceof Player targetPlayer) {
                if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                    continue;
                }
            }

            // Apply freeze effect
            freezeEntity(entity, 5); // 5 seconds freeze

            // Visual feedback
            entity.getWorld().spawnParticle(Particle.SNOWFLAKE, entity.getLocation().add(0, 1, 0),
                    20, 0.3, 0.5, 0.3, 0, null, true);
        }

        return true;
    }

    /**
     * Freeze the ground in a radius, creating ice blocks temporarily
     */
    private void freezeGround(Location center, double radius) {
        Set<Location> blocksToFreeze = new HashSet<>();

        // Find all valid blocks to freeze
        for (int x = (int) -radius; x <= radius; x++) {
            for (int z = (int) -radius; z <= radius; z++) {
                Location checkLoc = center.clone().add(x, 0, z);

                // Check if within circular radius
                if (checkLoc.distance(center) > radius) continue;

                // Find the top solid block
                Block block = checkLoc.getBlock();
                while (block.getY() > center.getY() - 5 && block.getType().isAir()) {
                    block = block.getRelative(0, -1, 0);
                }

                // Check if we can place ice on top
                Block iceBlock = block.getRelative(0, 1, 0);
                if (iceBlock.getType().isAir() && block.getType().isSolid()) {
                    blocksToFreeze.add(iceBlock.getLocation());
                }
            }
        }

        // Freeze the blocks
        for (Location loc : blocksToFreeze) {
            Block block = loc.getBlock();
            block.setType(Material.ICE);
            frozenBlocks.add(loc);
        }

        // Schedule ice removal after 10 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Location loc : blocksToFreeze) {
                    Block block = loc.getBlock();
                    if (block.getType() == Material.ICE) {
                        block.setType(Material.AIR);
                        // Small particle effect when ice melts
                        block.getWorld().spawnParticle(Particle.CLOUD, loc.add(0.5, 0.5, 0.5),
                                5, 0.3, 0.3, 0.3, 0, null, true);
                    }
                }
                frozenBlocks.removeAll(blocksToFreeze);
            }
        }.runTaskLater(plugin, 200L); // 10 seconds
    }

    /**
     * Freeze an entity, preventing movement
     */
    private void freezeEntity(LivingEntity entity, int durationSeconds) {
        long freezeUntil = System.currentTimeMillis() + (durationSeconds * 1000L);
        entity.setMetadata(META_NOVA_FROZEN, new FixedMetadataValue(plugin, freezeUntil));

        // Apply visual freeze
        entity.setFreezeTicks(entity.getMaxFreezeTicks());

        // Apply Slowness IV
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationSeconds * 20, 3, false, false, false));

        // Disable AI for mobs
        if (entity instanceof Mob mob) {
            mob.setAware(false);

            // Re-enable AI after freeze
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mob.isValid()) {
                        mob.setAware(true);
                    }
                }
            }.runTaskLater(plugin, durationSeconds * 20L);
        }

        // Cleanup metadata after duration
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isValid()) {
                    entity.removeMetadata(META_NOVA_FROZEN, plugin);
                }
            }
        }.runTaskLater(plugin, durationSeconds * 20L);
    }

    @Override
    public String getName() {
        return ChatColor.AQUA + "Frost Nova";
    }

    @Override
    public String getDescription() {
        return "Create an explosion of ice around you, freezing the ground and nearby enemies for 5 seconds.";
    }
}