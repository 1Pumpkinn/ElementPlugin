package saturn.elementPlugin.elements.impl.frost.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class FrostJoinListener implements Listener {
    private final ElementManager elementManager;

    public FrostJoinListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) == ElementType.FROST) {
            // Frost element doesn't need permanent potion effects on join
            // Speed effects are applied dynamically by FrostPassiveListener
        }
    }
}