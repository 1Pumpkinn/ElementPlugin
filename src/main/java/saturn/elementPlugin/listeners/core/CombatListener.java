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
 * FIXED: Now prevents PvP damage when players trust each other OR when target is in spawn
 * Priority check: Spawn protection > Trust system
 */
public class CombatListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;

    public CombatListener(ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
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

        final Player finalDamager = damager;

        // === PRIORITY 1: Check if victim is in spawn (protected zone) ===
        if (plugin.getDisabledRegionsManager().isInDisabledRegion(victim.getLocation())) {
            e.setCancelled(true);

            // Send message about spawn protection
            if (!finalDamager.hasMetadata("spawn_protection_cooldown")) {
                String regionName = plugin.getDisabledRegionsManager().getRegionNameAt(victim.getLocation());
                finalDamager.sendMessage(ChatColor.RED + "You cannot damage " +
                        victim.getName() + " - they are in a protected zone!" +
                        (regionName != null ? " (" + regionName + ")" : ""));

                // Add cooldown to prevent spam
                finalDamager.setMetadata("spawn_protection_cooldown",
                        new org.bukkit.metadata.FixedMetadataValue(plugin, true));

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    finalDamager.removeMetadata("spawn_protection_cooldown", plugin);
                }, 60L); // 3 second cooldown
            }
            return; // STOP HERE - don't check trust if in spawn
        }

        // === PRIORITY 2: Check trust system (only after spawn check passed) ===
        if (plugin.getTrustManager().trusts(victim, finalDamager)) {
            e.setCancelled(true);

            // Send message about trust
            if (!finalDamager.hasMetadata("trust_pvp_cooldown")) {
                finalDamager.sendMessage(ChatColor.YELLOW + "You cannot damage " +
                        victim.getName() + " - they trust you!");

                // Add cooldown to prevent spam
                finalDamager.setMetadata("trust_pvp_cooldown",
                        new org.bukkit.metadata.FixedMetadataValue(plugin, true));

                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    finalDamager.removeMetadata("trust_pvp_cooldown", plugin);
                }, 60L); // 3 second cooldown
            }
        }
    }
}