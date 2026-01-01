package saturn.elementPlugin.elements.impl.death.listeners;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.impl.death.DeathElement;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Death Upside 2: Go invisible for 10 seconds when on 2 hearts (Upgrade II)
 * Triggered automatically when health drops to 4.0 or below
 */
public class DeathInvisibilityListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    // Cooldown duration: 30 seconds
    private static final long COOLDOWN_MS = 30_000L;
    private static final double TRIGGER_HEALTH = 4.0; // 2 hearts
    private static final int INVIS_DURATION = 200; // 10 seconds (200 ticks)

    public DeathInvisibilityListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player has Death element
        var pd = elementManager.data(player.getUniqueId());
        if (pd.getCurrentElement() != ElementType.DEATH) return;

        // Check if player has Upgrade II
        if (pd.getUpgradeLevel(ElementType.DEATH) < 2) return;

        // Check if health will drop to trigger threshold
        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth > TRIGGER_HEALTH) return;
        if (finalHealth <= 0) return; // Don't trigger on death

        // Check cooldown
        if (player.hasMetadata(DeathElement.META_DEATH_INVIS_COOLDOWN)) {
            long cooldownEnd = player.getMetadata(DeathElement.META_DEATH_INVIS_COOLDOWN).get(0).asLong();
            if (System.currentTimeMillis() < cooldownEnd) {
                return; // Still on cooldown
            }
        }

        // Trigger invisibility after damage is applied
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && !player.isDead()) {
                activateInvisibility(player);
            }
        });
    }

    private void activateInvisibility(Player player) {
        // Set cooldown
        long cooldownEnd = System.currentTimeMillis() + COOLDOWN_MS;
        player.setMetadata(DeathElement.META_DEATH_INVIS_COOLDOWN, new FixedMetadataValue(plugin, cooldownEnd));

        // Apply invisibility
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.INVISIBILITY,
                INVIS_DURATION,
                0,
                false,
                false,
                false
        ));

        // Visual/audio feedback
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.getWorld().spawnParticle(
                org.bukkit.Particle.SMOKE,
                player.getLocation().add(0, 1, 0),
                30, 0.5, 0.5, 0.5, 0.1
        );

        player.sendMessage(org.bukkit.ChatColor.DARK_PURPLE + "✦ Death's Embrace activated! You are invisible for 10 seconds.");

        // Show remaining cooldown after invisibility ends
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.sendMessage(org.bukkit.ChatColor.GRAY + "Death's Embrace on cooldown (30 seconds)");
                }
            }
        }.runTaskLater(plugin, INVIS_DURATION);
    }
}