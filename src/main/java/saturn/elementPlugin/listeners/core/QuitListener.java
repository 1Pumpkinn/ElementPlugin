package saturn.elementPlugin.listeners.core;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.managers.ManaManager;
import saturn.elementPlugin.util.SmartEffectCleaner;
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

        // FIXED: Use SmartEffectCleaner to clear element effects before logout
        // This ensures a clean state when they log back in
        SmartEffectCleaner.clearForElementChange(plugin, player);

        // Save player data to ensure any changes are persisted
        PlayerData pd = plugin.getDataStore().getPlayerData(uuid);
        plugin.getDataStore().save(pd);

        plugin.getLogger().fine("Player " + player.getName() + " logged out - data saved and effects cleared");
    }
}