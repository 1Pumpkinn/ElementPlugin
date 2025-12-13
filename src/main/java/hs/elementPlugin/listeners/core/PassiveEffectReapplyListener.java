package hs.elementPlugin.listeners.core;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.util.SmartEffectCleaner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Listener that ensures element passive effects are reapplied in situations where they might be removed.
 * FIXED: Now uses SmartEffectCleaner for intelligent effect management
 *
 * This handles:
 * - Player respawn (after death)
 * - Totem of Undying usage (clears potion effects)
 * - Player join (ensures effects are present on login)
 */
public class PassiveEffectReapplyListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public PassiveEffectReapplyListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    /**
     * Reapply effects after player respawns from death
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        // Delay the effect reapplication to ensure the player is fully respawned
        scheduleReapplyWithValidation(player, 5L, "respawn");
    }

    /**
     * Reapply effects after player uses a Totem of Undying
     * Totems clear potion effects, so we need to restore element passives
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTotemUse(EntityResurrectEvent event) {
        // Only handle player resurrections
        if (!(event.getEntity() instanceof Player player)) return;

        // Delay slightly to let the totem effects apply first
        scheduleReapplyWithValidation(player, 10L, "totem usage");
    }

    /**
     * Reapply effects when player joins the server
     * FIXED: Now uses SmartEffectCleaner to validate effects
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay to ensure player is fully loaded
        scheduleReapplyWithValidation(player, 20L, "join");
    }

    /**
     * Schedule a task to reapply element passive effects with validation
     * FIXED: Uses SmartEffectCleaner to clean invalid effects
     *
     * @param player The player to reapply effects for
     * @param delayTicks Delay in ticks before reapplying
     * @param reason Reason for reapplying (for logging)
     */
    private void scheduleReapplyWithValidation(Player player, long delayTicks, String reason) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // FIXED: Use SmartEffectCleaner to remove invalid infinite effects
                    SmartEffectCleaner.cleanInvalidInfiniteEffects(plugin, player);

                    // Then reapply correct element effects
                    elementManager.applyUpsides(player);

                    plugin.getLogger().fine("Validated and reapplied element effects for " + player.getName() + " after " + reason);
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }
}