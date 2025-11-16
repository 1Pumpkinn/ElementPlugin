package hs.elementPlugin.listeners.player;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.attribute.Attribute;
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
 * This handles:
 * - Player respawn (after death)
 * - Totem of Undying usage (clears potion effects)
 * - Player join (ensures effects are present on login AND clears stacked effects)
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
     * CRITICAL: Clear ALL old effects first to prevent stacking
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Delay to ensure player is fully loaded
        scheduleReapplyWithValidation(player, 20L, "join");
    }


    /**
     * Schedule a task to reapply element passive effects
     *
     * @param player The player to reapply effects for
     * @param delayTicks Delay in ticks before reapplying
     * @param reason Reason for reapplying (for logging)
     */
    private void scheduleReapply(Player player, long delayTicks, String reason) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Reapply element effects
                    elementManager.applyUpsides(player);
                    plugin.getLogger().fine("Reapplied element passive effects for " + player.getName() + " after " + reason);
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }

    /**
     * Schedule a task to reapply element passive effects with validation
     * This clears ALL element effects first, then applies only the current element's effects
     * CRITICAL FIX: This prevents the reroller bug where old effects stack with new ones
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
                    // CRITICAL: Clear ALL element effects first
                    clearAllElementEffects(player);

                    // Then apply only the current element's effects
                    elementManager.applyUpsides(player);
                    plugin.getLogger().fine("Validated and reapplied element effects for " + player.getName() + " after " + reason);
                }
            }
        }.runTaskLater(plugin, delayTicks);
    }

    /**
     * Clear ALL possible element effects from a player
     * This prevents effect stacking when switching elements
     * CRITICAL FIX: This is the key to fixing the reroller disconnect bug
     */
    private void clearAllElementEffects(Player player) {
        // Get current element
        PlayerData pd = elementManager.data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement == null) {
            plugin.getLogger().warning("Attempted to clear effects for player " + player.getName() + " with no element");
            return;
        }

        // Clear effects from ALL elements (to prevent stacking)
        for (ElementType type : ElementType.values()) {
            // Skip the current element - we'll apply those effects next
            if (type == currentElement) continue;

            Element element = elementManager.get(type);
            if (element != null) {
                element.clearEffects(player);
            }
        }

        // Reset Life max health if player is NOT Life element
        if (currentElement != ElementType.LIFE) {
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null && attr.getBaseValue() > 20.0) {
                attr.setBaseValue(20.0);
                if (!player.isDead() && player.getHealth() > 20.0) {
                    player.setHealth(20.0);
                }
            }
        } else {
            // Ensure Life element has 30 HP
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null && attr.getBaseValue() != 30.0) {
                attr.setBaseValue(30.0);
                // Don't modify current health, just cap it if needed
                if (!player.isDead() && player.getHealth() > 30.0) {
                    player.setHealth(30.0);
                }
            }
        }
    }
}