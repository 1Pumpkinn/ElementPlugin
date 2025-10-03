package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class RespawnListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public RespawnListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        
        // Delay the effect reapplication to ensure the player is fully respawned
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    // Reapply element effects after respawn
                    elementManager.applyUpsides(player);
                }
            }
        }.runTaskLater(plugin, 5L); // 0.25 second delay
    }
}
