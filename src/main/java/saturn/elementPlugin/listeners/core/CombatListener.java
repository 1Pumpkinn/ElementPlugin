package saturn.elementPlugin.listeners.core;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * FIXED: Now prevents PvP damage when players trust each other
 */
public class CombatListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;

    // Constructor for compatibility - accepts both plugin and elementManager
    public CombatListener(ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

    // Legacy constructor for backward compatibility
    @Deprecated
    public CombatListener(ElementManager elements) {
        this.plugin = null; // Will cause issues, but prevents compile error
        this.elements = elements;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;

        Player damager = null;

        // Direct player damage
        if (e.getDamager() instanceof Player p) {
            damager = p;
        }
        // Projectile damage
        else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            damager = p;
        }

        if (damager == null) return;

        // Store damager in final variable for lambda
        final Player finalDamager = damager;

        // CRITICAL FIX: Check if VICTIM trusts DAMAGER (not the other way around!)
        // The victim's trust list determines who can't hurt them
        if (plugin.getTrustManager().trusts(victim, finalDamager)) {
            e.setCancelled(true);

            // Optional: Send feedback message (only once per few seconds to avoid spam)
            if (!finalDamager.hasMetadata("trust_pvp_cooldown")) {
                finalDamager.sendMessage(ChatColor.YELLOW + "You cannot damage " +
                        victim.getName() + " - they trust you!");

                // Add cooldown metadata to prevent spam
                finalDamager.setMetadata("trust_pvp_cooldown",
                        new org.bukkit.metadata.FixedMetadataValue(plugin, true));

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    finalDamager.removeMetadata("trust_pvp_cooldown", plugin);
                }, 60L); // 3 second cooldown
            }
        }
    }
}