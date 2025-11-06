package hs.elementPlugin.elements.impl.frost.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.impl.frost.FrostFrozenPunchAbility;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Handles the Frozen Punch ability effect when a player hits an entity
 */
public class FrostFrozenPunchListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;
    private final TrustManager trustManager;

    // Metadata key for frozen entities
    public static final String META_FROZEN = "frost_frozen";

    public FrostFrozenPunchListener(ElementPlugin plugin, ElementManager elementManager, TrustManager trustManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        this.trustManager = trustManager;
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if damager is a Frost element player
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        // Check if player has Frost element
        if (elementManager.getPlayerElement(player) != ElementType.FROST) {
            return;
        }

        // Check if player has Frozen Punch ready
        if (!player.hasMetadata(FrostFrozenPunchAbility.META_FROZEN_PUNCH_READY)) {
            return;
        }

        long readyUntil = player.getMetadata(FrostFrozenPunchAbility.META_FROZEN_PUNCH_READY).get(0).asLong();
        if (System.currentTimeMillis() > readyUntil) {
            // Ability expired
            player.removeMetadata(FrostFrozenPunchAbility.META_FROZEN_PUNCH_READY, plugin);
            return;
        }

        // Check if target is a living entity
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // Don't freeze trusted players
        if (target instanceof Player targetPlayer) {
            if (trustManager.isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                return;
            }
        }

        // Consume the ability
        player.removeMetadata(FrostFrozenPunchAbility.META_FROZEN_PUNCH_READY, plugin);

        // Apply freeze effect
        freezeEntity(target);

        // Visual and audio feedback
        Location loc = target.getLocation().add(0, 1, 0);
        target.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 30, 0.5, 0.5, 0.5, 0.1, null, true);
        target.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, loc, 20, 0.3, 0.5, 0.3, 0.1, null, true);
        target.getWorld().playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 2.0f);

        player.sendMessage(ChatColor.AQUA + "Target frozen!");
    }

    /**
     * Freeze an entity in place for 5 seconds
     */
    private void freezeEntity(LivingEntity entity) {
        // Set frozen metadata
        long frozenUntil = System.currentTimeMillis() + 5000L; // 5 seconds
        entity.setMetadata(META_FROZEN, new FixedMetadataValue(plugin, frozenUntil));

        // Apply slowness and jump boost to prevent movement
        entity.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 255, true, true, true)); // 5 seconds
        entity.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 128, true, true, true)); // Prevent jumping

        // Set velocity to zero
        entity.setVelocity(new Vector(0, 0, 0));

        // Spawn continuous freeze particles
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!entity.isValid() || ticks >= 100) {
                    // Remove frozen metadata when done
                    entity.removeMetadata(META_FROZEN, plugin);
                    cancel();
                    return;
                }

                // Check if freeze expired early
                if (entity.hasMetadata(META_FROZEN)) {
                    long until = entity.getMetadata(META_FROZEN).get(0).asLong();
                    if (System.currentTimeMillis() > until) {
                        entity.removeMetadata(META_FROZEN, plugin);
                        cancel();
                        return;
                    }
                } else {
                    cancel();
                    return;
                }

                // Spawn freeze particles
                Location loc = entity.getLocation().add(0, 1, 0);
                entity.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 5, 0.3, 0.5, 0.3, 0, null, true);

                // Reset velocity to keep entity frozen
                entity.setVelocity(new Vector(0, entity.getVelocity().getY(), 0));

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}