package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        // Immediate write of player data on quit
        mana.save(uuid);
        // Note: DataStore.save() is synchronous and flushes to disk.
        // If you later move to async writes, ensure awaiting in-flight writes here.
    }
}
