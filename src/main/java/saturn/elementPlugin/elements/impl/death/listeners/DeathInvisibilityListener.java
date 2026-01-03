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
import org.bukkit.event.entity.EntityResurrectEvent;
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

        // Reset passive if healed above threshold
        if (finalHealth > TRIGGER_HEALTH) {
            player.removeMetadata(META_ACTIVE, plugin);
            player.removeMetadata(META_END_TIME, plugin);
            return;
        }

        // Already triggered while low HP
        if (player.hasMetadata(META_ACTIVE)) return;

        // Trigger passive
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                activate(player);
            }
        });
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
            // Check all online players for active Death invisibility
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.equals(joiningPlayer)) continue;

                // If they have Death element invisibility active, hide their equipment to the new player
                if (onlinePlayer.hasMetadata(META_ACTIVE)) {
                    var data = elementManager.data(onlinePlayer.getUniqueId());
                    if (data.getCurrentElement() == ElementType.DEATH &&
                            data.getUpgradeLevel(ElementType.DEATH) >= 2) {

                        // Hide this Death player's equipment to the joining player
                        Map<EquipmentSlot, ItemStack> empty = new EnumMap<>(EquipmentSlot.class);
                        empty.put(EquipmentSlot.HEAD, null);
                        empty.put(EquipmentSlot.CHEST, null);
                        empty.put(EquipmentSlot.LEGS, null);
                        empty.put(EquipmentSlot.FEET, null);
                        empty.put(EquipmentSlot.HAND, null);
                        empty.put(EquipmentSlot.OFF_HAND, null);

                        try {
                            joiningPlayer.sendEquipmentChange(onlinePlayer, empty);
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to hide equipment for " + onlinePlayer.getName() +
                                    " from joining player " + joiningPlayer.getName() + ": " + e.getMessage());
                        }
                    }
                }
            }
        }, 20L); // 1 second delay
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