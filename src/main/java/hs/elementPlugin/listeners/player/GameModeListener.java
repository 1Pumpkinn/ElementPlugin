package hs.elementPlugin.listeners.player;

import hs.elementPlugin.managers.ManaManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GameModeListener implements Listener {
    private static final int MAX_MANA = 100;

    private final ManaManager manaManager;

    public GameModeListener(ManaManager manaManager) {
        this.manaManager = manaManager;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();
        GameMode newMode = e.getNewGameMode();

        // When entering creative, fill mana
        if (newMode == GameMode.CREATIVE) {
            var pd = manaManager.get(p.getUniqueId());
            pd.setMana(MAX_MANA);
        }
    }
}