package saturn.elementPlugin.elements.impl.death.listeners;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Death Upgrade II (Passive):
 * Go invisible (armor and held items visually hidden, protection kept)
 * once when dropping to 2 hearts.
 * Resets when healed above threshold.
 * Reapplies if totem pops during invisibility.
 * FIXED: Prevents random armor reappearing
 */
public class DeathInvisibilityListener implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    private static final double TRIGGER_HEALTH = 4.0; // 2 hearts
    private static final int INVIS_DURATION_TICKS = 200; // 10 seconds

    private static final String META_ACTIVE = "death_invis_active";
    private static final String META_END_TIME = "death_invis_end_time";

    public DeathInvisibilityListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        var data = elementManager.data(player.getUniqueId());
        if (data.getCurrentElement() != ElementType.DEATH) return;
        if (data.getUpgradeLevel(ElementType.DEATH) < 2) return;

        double finalHealth = player.getHealth() - event.getFinalDamage();

        // Death check
        if (finalHealth <= 0) return;

        // CRITICAL FIX: Check if invisibility is already active FIRST
        // This prevents flickering when getting hit while invisible
        if (player.hasMetadata(META_ACTIVE)) {
            // Still active - check if we should reset it due to healing
            if (finalHealth > TRIGGER_HEALTH) {
                // Healed above threshold while invisible - this shouldn't happen often
                // but if it does, we'll let the invisibility finish naturally
            }
            return; // Don't retrigger
        }

        // Not active yet - check if we should trigger
        if (finalHealth <= TRIGGER_HEALTH) {
            // Trigger passive
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline() && !player.isDead()) {
                    activate(player);
                }
            });
        }
    }

    /**
     * When totem pops during invisibility, reapply the invisibility effects
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player has active Death invisibility
        if (!player.hasMetadata(META_ACTIVE)) return;
        if (!player.hasMetadata(META_END_TIME)) return;

        var data = elementManager.data(player.getUniqueId());
        if (data.getCurrentElement() != ElementType.DEATH) return;
        if (data.getUpgradeLevel(ElementType.DEATH) < 2) return;

        // Get remaining time
        long endTime = player.getMetadata(META_END_TIME).get(0).asLong();
        long currentTime = System.currentTimeMillis();
        long remainingMs = endTime - currentTime;

        if (remainingMs <= 0) {
            // Invisibility already expired, don't reapply
            player.removeMetadata(META_ACTIVE, plugin);
            player.removeMetadata(META_END_TIME, plugin);
            return;
        }

        // Reapply invisibility after totem effects (delay to let totem apply first)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;

            int remainingTicks = (int) (remainingMs / 50); // Convert ms to ticks

            // Reapply invisibility potion
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    remainingTicks,
                    0,
                    false,
                    false,
                    false
            ));

            // Re-hide equipment
            hideEquipment(player);

        }, 10L); // 0.5 second delay after totem
    }

    /**
     * When a player joins, hide equipment of any currently invisible Death players
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player joiningPlayer = event.getPlayer();

        // Delay slightly to ensure player is fully loaded
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!joiningPlayer.isOnline()) return;

            // Check all online players for active Death invisibility
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.equals(joiningPlayer)) continue;

                // If they have Death element invisibility active, hide their equipment to the new player
                if (onlinePlayer.hasMetadata(META_ACTIVE)) {
                    var data = elementManager.data(onlinePlayer.getUniqueId());
                    if (data.getCurrentElement() == ElementType.DEATH &&
                            data.getUpgradeLevel(ElementType.DEATH) >= 2) {

                        hideEquipmentTo(onlinePlayer, joiningPlayer);
                    }
                }
            }
        }, 20L); // 1 second delay
    }

    /**
     * Monitor equipment changes during invisibility
     * CRITICAL: Re-hide equipment when player changes held items
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquipmentChange(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        if (!player.hasMetadata(META_ACTIVE)) return;

        // Re-hide equipment to all viewers after short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.hasMetadata(META_ACTIVE)) {
                hideEquipment(player);
            }
        }, 2L);
    }

    /**
     * Monitor armor changes during invisibility
     * CRITICAL: Re-hide equipment when player changes armor
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onArmorChange(com.destroystokyo.paper.event.player.PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();

        if (!player.hasMetadata(META_ACTIVE)) return;

        // Re-hide equipment to all viewers after short delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && player.hasMetadata(META_ACTIVE)) {
                hideEquipment(player);
            }
        }, 2L);
    }

    /**
     * Monitor when invisibility potion effect is removed/expires
     * CRITICAL: Show equipment when invisibility ends
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPotionEffectEnd(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Only care about invisibility effect removal
        if (event.getModifiedType() != PotionEffectType.INVISIBILITY) return;
        if (event.getAction() != EntityPotionEffectEvent.Action.REMOVED &&
                event.getAction() != EntityPotionEffectEvent.Action.CLEARED) return;

        // Check if this is Death invisibility ending
        if (!player.hasMetadata(META_ACTIVE)) return;

        var data = elementManager.data(player.getUniqueId());
        if (data.getCurrentElement() != ElementType.DEATH) return;

        // Check if the invisibility should still be active
        if (player.hasMetadata(META_END_TIME)) {
            long endTime = player.getMetadata(META_END_TIME).get(0).asLong();
            long currentTime = System.currentTimeMillis();

            if (currentTime < endTime) {
                // Invisibility should still be active - reapply potion
                int remainingTicks = (int) ((endTime - currentTime) / 50);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && player.hasMetadata(META_ACTIVE)) {
                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.INVISIBILITY,
                                remainingTicks,
                                0,
                                false,
                                false,
                                false
                        ));
                        hideEquipment(player);
                    }
                }, 1L);
            }
        }
    }

    private void activate(Player player) {
        // Mark as active so it doesn't spam
        player.setMetadata(META_ACTIVE, new FixedMetadataValue(plugin, true));

        // Store end time for totem reapplication
        long endTime = System.currentTimeMillis() + (INVIS_DURATION_TICKS * 50L); // Convert ticks to ms
        player.setMetadata(META_END_TIME, new FixedMetadataValue(plugin, endTime));

        // Potion invisibility
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                INVIS_DURATION_TICKS,
                0,
                false,
                false,
                false
        ));

        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        player.getWorld().spawnParticle(
                Particle.SMOKE,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1
        );

        // Hide equipment visually after a small delay
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                hideEquipment(player);

                // Start periodic re-hiding to prevent armor from randomly appearing
                startPeriodicHiding(player);
            }
        }, 2L);

        // Restore visuals after invis ends
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;

                // Clean up metadata
                player.removeMetadata(META_ACTIVE, plugin);
                player.removeMetadata(META_END_TIME, plugin);

                showEquipment(player);
            }
        }.runTaskLater(plugin, INVIS_DURATION_TICKS + 2L);
    }

    /**
     * CRITICAL FIX: Periodically re-hide equipment during invisibility
     * This prevents random armor reappearing due to various events
     */
    private void startPeriodicHiding(Player player) {
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                // Stop if player logged out or invisibility ended
                if (!player.isOnline() || !player.hasMetadata(META_ACTIVE)) {
                    cancel();
                    return;
                }

                // Stop if time expired
                if (player.hasMetadata(META_END_TIME)) {
                    long endTime = player.getMetadata(META_END_TIME).get(0).asLong();
                    if (System.currentTimeMillis() >= endTime) {
                        cancel();
                        return;
                    }
                }

                // Re-hide equipment every 2 ticks (0.1 seconds)
                hideEquipment(player);

                ticks += 2;
            }
        }.runTaskTimer(plugin, 2L, 2L); // Run every 2 ticks (0.1 seconds)
    }

    /* -------------------- EQUIPMENT VISUALS -------------------- */

    private void hideEquipment(Player target) {
        Map<EquipmentSlot, ItemStack> empty = new EnumMap<>(EquipmentSlot.class);
        empty.put(EquipmentSlot.HEAD, null);
        empty.put(EquipmentSlot.CHEST, null);
        empty.put(EquipmentSlot.LEGS, null);
        empty.put(EquipmentSlot.FEET, null);
        empty.put(EquipmentSlot.HAND, null);
        empty.put(EquipmentSlot.OFF_HAND, null);

        // Send to ALL other players (including spectators)
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            try {
                viewer.sendEquipmentChange(target, empty);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hide equipment for " + target.getName() + " from " + viewer.getName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Hide equipment to a specific viewer (used when players join)
     */
    private void hideEquipmentTo(Player target, Player viewer) {
        Map<EquipmentSlot, ItemStack> empty = new EnumMap<>(EquipmentSlot.class);
        empty.put(EquipmentSlot.HEAD, null);
        empty.put(EquipmentSlot.CHEST, null);
        empty.put(EquipmentSlot.LEGS, null);
        empty.put(EquipmentSlot.FEET, null);
        empty.put(EquipmentSlot.HAND, null);
        empty.put(EquipmentSlot.OFF_HAND, null);

        try {
            viewer.sendEquipmentChange(target, empty);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to hide equipment for " + target.getName() + " from " + viewer.getName() + ": " + e.getMessage());
        }
    }

    private void showEquipment(Player target) {
        if (!target.isOnline()) return;

        var inv = target.getInventory();

        Map<EquipmentSlot, ItemStack> real = new EnumMap<>(EquipmentSlot.class);
        real.put(EquipmentSlot.HEAD, inv.getHelmet());
        real.put(EquipmentSlot.CHEST, inv.getChestplate());
        real.put(EquipmentSlot.LEGS, inv.getLeggings());
        real.put(EquipmentSlot.FEET, inv.getBoots());
        real.put(EquipmentSlot.HAND, inv.getItemInMainHand());
        real.put(EquipmentSlot.OFF_HAND, inv.getItemInOffHand());

        // Send to ALL other players
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            try {
                viewer.sendEquipmentChange(target, real);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to show equipment for " + target.getName() + " to " + viewer.getName() + ": " + e.getMessage());
            }
        }
    }
}