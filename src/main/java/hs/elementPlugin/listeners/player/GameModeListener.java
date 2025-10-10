package hs.elementPlugin.listeners.player;

import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;

public class GameModeListener implements Listener {
    private final ManaManager manaManager;
    private final ConfigManager configManager;

    public GameModeListener(ManaManager manaManager, ConfigManager configManager) {
        this.manaManager = manaManager;
        this.configManager = configManager;
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        Player p = e.getPlayer();
        GameMode newMode = e.getNewGameMode();

        // When entering creative, fill mana
        if (newMode == GameMode.CREATIVE) {
            int maxMana = configManager.getMaxMana();
            var pd = manaManager.get(p.getUniqueId());
            pd.setMana(maxMana);
        }
        // When leaving creative (to survival/adventure/spectator), mana stays at current level
        // Normal regen will take over from ManaManager's tick
    }
}