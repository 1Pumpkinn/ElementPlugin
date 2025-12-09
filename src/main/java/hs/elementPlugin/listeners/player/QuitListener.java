package hs.elementPlugin.listeners.player;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class QuitListener implements Listener {
    private final ElementPlugin plugin;
    private final ManaManager mana;

    public QuitListener(ElementPlugin plugin, ManaManager mana) {
        this.plugin = plugin;
        this.mana = mana;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        // CRITICAL: Cancel any ongoing rolling animation
        if (plugin.getElementManager().isCurrentlyRolling(player)) {
            plugin.getLogger().info("Player " + player.getName() + " disconnected during element roll - cancelling animation");
            plugin.getElementManager().cancelRolling(player);
        }

        // Save mana data
        mana.save(uuid);

        // CRITICAL: Clear ALL element effects before player logs out
        // This prevents effects from persisting or stacking after reroll
        clearAllElementEffects(player);

        // Save player data to ensure any changes are persisted
        PlayerData pd = plugin.getDataStore().getPlayerData(uuid);
        plugin.getDataStore().save(pd);

        plugin.getLogger().fine("Player " + player.getName() + " logged out - data saved and effects cleared");
    }

    /**
     * CRITICAL: Clear ALL possible element effects from a player on logout
     * This prevents old element effects from persisting after reroll during logout
     */
    private void clearAllElementEffects(Player player) {
        PlayerData pd = plugin.getElementManager().data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        // Clear effects from ALL elements (not just current)
        // This ensures no leftover effects if they were rerolling when they logged out
        for (ElementType type : ElementType.values()) {
            Element element = plugin.getElementManager().get(type);
            if (element != null) {
                try {
                    element.clearEffects(player);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Error clearing " + type + " effects for " + player.getName() + ": " + ex.getMessage());
                }
            }
        }

        // Reset max health to default 20 HP while preserving current health
        // Their current health value is preserved - only max health is adjusted
        try {
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null && attr.getBaseValue() > 20.0) {
                // Store current health before changing max health
                double currentHealth = player.getHealth();
                attr.setBaseValue(20.0);

                // Restore current health (capped at new max if necessary)
                // This preserves low health states (e.g., if player is about to die)
                if (!player.isDead() && currentHealth > 0) {
                    player.setHealth(Math.min(currentHealth, 20.0));
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Error resetting max health for " + player.getName() + ": " + ex.getMessage());
        }

        plugin.getLogger().fine("Cleared all element effects for " + player.getName() + " (Current: " + currentElement + ")");
    }
}