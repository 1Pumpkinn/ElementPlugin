package saturn.elementPlugin.listeners.core;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.managers.ManaManager;
import saturn.elementPlugin.managers.trust.TrustManager;
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
    private final TrustManager trust;

    public QuitListener(ElementPlugin plugin, ManaManager mana, TrustManager trust) {
        this.plugin = plugin;
        this.mana = mana;
        this.trust = trust;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID uuid = player.getUniqueId();

        // Handle trust cleanup
        trust.handlePlayerQuit(uuid);

        // CRITICAL: Cancel any ongoing rolling animation
        if (plugin.getElementManager().isCurrentlyRolling(player)) {
            plugin.getLogger().info("Player " + player.getName() + " disconnected during element roll - cancelling animation");
            plugin.getElementManager().cancelRolling(player);
        }

        // Save mana data
        mana.save(uuid);

        // Clear element effects before logout
        SmartEffectCleaner.clearForElementChange(plugin, player);

        // Save player data to ensure any changes are persisted
        PlayerData pd = plugin.getDataStore().getPlayerData(uuid);
        plugin.getDataStore().save(pd);

        plugin.getLogger().fine("Player " + player.getName() + " logged out - data saved and effects cleared");
    }
}