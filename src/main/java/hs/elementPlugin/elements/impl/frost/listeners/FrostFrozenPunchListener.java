package hs.elementPlugin.elements.impl.frost.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.impl.frost.FrostPunchAbility;
import hs.elementPlugin.managers.ElementManager;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FrostFrozenPunchListener implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public static final String META_FROZEN = "frost_frozen";

    public FrostFrozenPunchListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;


        // Check if attacker has Frost element
        ElementType type = elementManager.getPlayerElement(attacker);
        if (type != ElementType.FROST) {

            return;
        }

        // Check if frozen punch metadata exists
        if (!attacker.hasMetadata(FrostPunchAbility.META_FROZEN_PUNCH_READY)) {

            return;
        }

        long until = attacker.getMetadata(FrostPunchAbility.META_FROZEN_PUNCH_READY).get(0).asLong();
        if (System.currentTimeMillis() > until) {
            attacker.removeMetadata(FrostPunchAbility.META_FROZEN_PUNCH_READY, plugin);

            return;
        }

        // Don't freeze trusted players
        if (victim instanceof Player targetPlayer &&
                plugin.getTrustManager().isTrusted(attacker.getUniqueId(), targetPlayer.getUniqueId())) {

            return;
        }

        // Remove the ready state (consume ability)
        attacker.removeMetadata(FrostPunchAbility.META_FROZEN_PUNCH_READY, plugin);

        // Apply freeze effect
        applyFreezeEffect(victim);

        // Feedback
        Location hitLoc = victim.getEyeLocation();
        victim.getWorld().spawnParticle(Particle.SNOWFLAKE, hitLoc, 50, 0.3, 0.5, 0.3, 0.1, null, true);
        victim.getWorld().spawnParticle(Particle.CLOUD, hitLoc, 25, 0.3, 0.5, 0.3, 0.05, null, true);
        victim.getWorld().playSound(hitLoc, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);

    }

    private void applyFreezeEffect(LivingEntity entity) {
        long freezeUntil = System.currentTimeMillis() + 5000L; // 5 seconds
        entity.setMetadata(META_FROZEN, new FixedMetadataValue(plugin, freezeUntil));

        entity.setFreezeTicks(entity.getMaxFreezeTicks());
        entity.setVelocity(new Vector(0, 0, 0));

        if (entity instanceof Mob mob) {
            mob.setAware(true);
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (mob.isValid()) mob.setAware(true);
                }
            }.runTaskLater(plugin, 100L);
        }

        // Visual freeze effect - continuously apply freeze and prevent movement
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!entity.isValid() || ticks >= 100) {
                    entity.removeMetadata(META_FROZEN, plugin);
                    cancel();
                    return;
                }
                if (!entity.hasMetadata(META_FROZEN)) {
                    cancel();
                    return;
                }
                long until = entity.getMetadata(META_FROZEN).get(0).asLong();
                if (System.currentTimeMillis() > until) {
                    entity.removeMetadata(META_FROZEN, plugin);
                    cancel();
                    return;
                }

                // Keep entity frozen and motionless
                entity.setFreezeTicks(entity.getMaxFreezeTicks());
                entity.setVelocity(new Vector(0, 0, 0));

                if (ticks % 10 == 0) {
                    Location loc = entity.getLocation().add(0, 1, 0);
                    entity.getWorld().spawnParticle(Particle.SNOWFLAKE, loc, 5, 0.3, 0.3, 0.3, 0, null, true);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Metadata cleanup
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isValid()) {
                    entity.removeMetadata(META_FROZEN, plugin);
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!player.hasMetadata(META_FROZEN)) return;

        long until = player.getMetadata(META_FROZEN).get(0).asLong();
        if (System.currentTimeMillis() > until) {
            player.removeMetadata(META_FROZEN, plugin);
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to != null && (from.getX() != to.getX() || from.getZ() != to.getZ())) {
            event.setTo(from);
            player.setVelocity(new Vector(0, player.getVelocity().getY(), 0));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMobMove(EntityMoveEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player) return;

        if (!entity.hasMetadata(META_FROZEN)) return;

        long until = entity.getMetadata(META_FROZEN).get(0).asLong();
        if (System.currentTimeMillis() > until) {
            entity.removeMetadata(META_FROZEN, plugin);
            return;
        }

        event.setCancelled(true);
    }
}
