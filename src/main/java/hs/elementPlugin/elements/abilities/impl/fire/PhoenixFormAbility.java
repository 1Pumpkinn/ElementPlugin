package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fire element's Phoenix Form ability - PASSIVE revive with 1 HP and become invincible/invisible
 */
public class PhoenixFormAbility extends BaseAbility implements Listener {
    private final ElementPlugin plugin;

    // Metadata keys
    public static final String META_PHOENIX_INVULNERABLE = "fire_phoenix_invulnerable";

    // Cooldown tracking (5 minutes = 300 seconds)
    private static final long COOLDOWN_DURATION_MS = 5 * 60 * 1000L; // 5 minutes in milliseconds
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public PhoenixFormAbility(ElementPlugin plugin) {
        super("fire_phoenix_form", 75, 0, 2);
        this.plugin = plugin;

        // Register this as a listener to handle death prevention
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Show cooldown status when player tries to use it
        if (isOnCooldown(player)) {
            long remainingMs = getRemainingCooldown(player);
            int remainingMinutes = (int) (remainingMs / 60000);
            int remainingSeconds = (int) ((remainingMs % 60000) / 1000);

            player.sendMessage(ChatColor.RED + "Phoenix Form is on cooldown for " +
                    remainingMinutes + "m " + remainingSeconds + "s");
        } else {

        }
        return false; // Don't consume the ability since it's passive
    }

    /**
     * Handle death prevention and phoenix resurrection - ALWAYS ACTIVE
     * PRIORITY.LOWEST means this runs FIRST, before totem
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if this damage would be fatal
        if (player.getHealth() - event.getFinalDamage() > 0) return;

        // CRITICAL FIX: Check if player currently has Fire element
        if (!hasFireElement(player)) {
            return; // Player doesn't have Fire element - don't trigger
        }

        // Check if ability is on cooldown
        if (isOnCooldown(player)) {
            // Phoenix Form is on cooldown - let death/totem happen
            return;
        }

        // TRIGGER PHOENIX FORM AUTOMATICALLY
        event.setCancelled(true); // Prevent death

        // Set health to 1
        player.setHealth(1.0);

        // Start cooldown
        startCooldown(player);

        // Create explosion effect
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_GHAST_DEATH, 2.0f, 1.0f);
        player.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.8f);

        // Massive particle explosion
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(0, 1, 0), 100, 1.0, 1.0, 1.0, 0.2, null, true);
        player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 100, 1.0, 1.0, 1.0, 0.2, null, true);
        player.getWorld().spawnParticle(Particle.LAVA, loc.clone().add(0, 1, 0), 30, 1.0, 1.0, 1.0, 0, null, true);
        player.getWorld().spawnParticle(Particle.EXPLOSION, loc.clone().add(0, 1, 0), 5, 0.5, 0.5, 0.5, 0, null, true);

        // Damage nearby enemies
        for (LivingEntity entity : loc.getNearbyLivingEntities(5.0)) {
            if (entity.equals(player)) continue;

            // Don't damage trusted players
            if (entity instanceof Player targetPlayer) {
                // We need to check trust, but we don't have context here
                // So we'll skip this check for now
            }

            entity.damage(8.0, player); // Deal 4 hearts of damage
            entity.setFireTicks(100); // Set on fire for 5 seconds

            // Knockback
            org.bukkit.util.Vector knockback = entity.getLocation().toVector()
                    .subtract(loc.toVector())
                    .normalize()
                    .multiply(1.5)
                    .setY(0.5);
            entity.setVelocity(knockback);
        }

        // Apply invincibility and invisibility
        applyPhoenixResurrection(player);

        // Notify player - SINGLE MESSAGE
        player.sendTitle(
                ChatColor.GOLD + "PHOENIX FORM",
                ChatColor.YELLOW + "Invincible for 3s | 5min cooldown",
                10, 40, 10
        );
    }

    /**
     * CRITICAL FIX: Check if player currently has Fire element
     */
    private boolean hasFireElement(Player player) {
        var pd = plugin.getElementManager().data(player.getUniqueId());
        return pd.getCurrentElement() == hs.elementPlugin.elements.ElementType.FIRE;
    }

    /**
     * Apply resurrection effects: invincibility, invisibility, and particle trail
     */
    private void applyPhoenixResurrection(Player player) {
        int durationTicks = 60; // 3 seconds

        // Mark player as invulnerable
        player.setMetadata(META_PHOENIX_INVULNERABLE, new FixedMetadataValue(plugin, System.currentTimeMillis() + 3000L));

        // Apply invisibility
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                durationTicks,
                0,
                false, false, false
        ));

        // Apply damage resistance for extra protection
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE,
                durationTicks,
                255, // Maximum resistance
                false, false, false
        ));

        // Particle trail task
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= durationTicks) {
                    player.removeMetadata(META_PHOENIX_INVULNERABLE, plugin);
                    cancel();
                    return;
                }

                // Spawn phoenix fire particles around player
                Location loc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(
                        Particle.SOUL_FIRE_FLAME,
                        loc, 10, 0.3, 0.5, 0.3, 0.05, null, true
                );

                // Wings effect
                if (ticks % 5 == 0) {
                    for (int i = 0; i < 2; i++) {
                        double angle = Math.toRadians(i * 180);
                        for (double r = 0; r < 2; r += 0.3) {
                            double x = Math.cos(angle) * r;
                            double z = Math.sin(angle) * r;
                            Location wingLoc = loc.clone().add(x, 0, z);
                            player.getWorld().spawnParticle(
                                    Particle.FLAME,
                                    wingLoc, 1, 0, 0, 0, 0, null, true
                            );
                        }
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Notification when effect ends
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage(ChatColor.YELLOW + "Phoenix Form protection has ended.");
                    player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 1.0f);
                }
            }
        }.runTaskLater(plugin, durationTicks);
    }

    /**
     * Prevent damage while invulnerable
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamageDuringInvulnerability(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (player.hasMetadata(META_PHOENIX_INVULNERABLE)) {
            long invulnerableUntil = player.getMetadata(META_PHOENIX_INVULNERABLE).get(0).asLong();
            if (System.currentTimeMillis() < invulnerableUntil) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Prevent totem consumption when Phoenix Form triggers
     * This MUST run at LOWEST priority to prevent totem from triggering first
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onTotemUse(org.bukkit.event.entity.EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // CRITICAL FIX: Check if player currently has Fire element
        if (!hasFireElement(player)) {
            return; // Player doesn't have Fire element - allow totem
        }

        // Check if Phoenix Form just triggered
        if (player.hasMetadata(META_PHOENIX_INVULNERABLE)) {
            // Phoenix Form just activated - cancel totem usage
            event.setCancelled(true);
            return;
        }

        // Also prevent totem if Phoenix Form is off cooldown
        // (meaning it WILL trigger in the damage handler)
        if (!isOnCooldown(player)) {
            event.setCancelled(true);
        }
    }

    // Cooldown management
    private void startCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COOLDOWN_DURATION_MS);
    }

    private boolean isOnCooldown(Player player) {
        Long cooldownEnd = cooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return false;

        if (System.currentTimeMillis() >= cooldownEnd) {
            cooldowns.remove(player.getUniqueId());
            return false;
        }

        return true;
    }

    private long getRemainingCooldown(Player player) {
        Long cooldownEnd = cooldowns.get(player.getUniqueId());
        if (cooldownEnd == null) return 0;

        return Math.max(0, cooldownEnd - System.currentTimeMillis());
    }

    /**
     * CRITICAL FIX: Clear cooldown when player changes elements
     * This should be called from FireElement.clearEffects()
     */
    public void clearCooldown(Player player) {
        cooldowns.remove(player.getUniqueId());
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Phoenix Form";
    }

    @Override
    public String getDescription() {
        return "PASSIVE: When you would die, automatically survive with 1 HP and explode in flames, becoming invisible and invincible for 3 seconds. 5 minute cooldown.";
    }
}