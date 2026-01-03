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
 * Go invisible (armor visually hidden, protection kept)
 * once when dropping to 2 hearts.
 * Resets when healed above threshold.
 */
public class DeathInvisibilityListener implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    private static final double TRIGGER_HEALTH = 4.0; // 2 hearts
    private static final int INVIS_DURATION_TICKS = 200; // 10 seconds

    private static final String META_ACTIVE = "death_invis_active";

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

    private void activate(Player player) {
        // Mark as active so it doesn't spam
        player.setMetadata(META_ACTIVE, new FixedMetadataValue(plugin, true));

        // Potion invisibility
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                INVIS_DURATION_TICKS,
                0,
                false,
                false,
                false
        ));

        // Hide armor visually
        hideArmor(player);

        // Effects
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.5f);
        player.getWorld().spawnParticle(
                Particle.SMOKE,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1
        );

        // Restore visuals after invis ends
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                showArmor(player);
                player.sendMessage(ChatColor.DARK_GRAY + "Death shrouds you...");
            }
        }.runTaskLater(plugin, INVIS_DURATION_TICKS);
    }

    /* -------------------- ARMOR VISUALS -------------------- */

    private void hideArmor(Player target) {
        Map<EquipmentSlot, ItemStack> empty = new EnumMap<>(EquipmentSlot.class);
        empty.put(EquipmentSlot.HEAD, null);
        empty.put(EquipmentSlot.CHEST, null);
        empty.put(EquipmentSlot.LEGS, null);
        empty.put(EquipmentSlot.FEET, null);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            viewer.sendEquipmentChange(target, empty);
        }
    }

    private void showArmor(Player target) {
        var inv = target.getInventory();

        Map<EquipmentSlot, ItemStack> real = new EnumMap<>(EquipmentSlot.class);
        real.put(EquipmentSlot.HEAD, inv.getHelmet());
        real.put(EquipmentSlot.CHEST, inv.getChestplate());
        real.put(EquipmentSlot.LEGS, inv.getLeggings());
        real.put(EquipmentSlot.FEET, inv.getBoots());

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            viewer.sendEquipmentChange(target, real);
        }
    }
}
