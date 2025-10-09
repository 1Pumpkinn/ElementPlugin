package hs.elementPlugin.elements.impl.fire.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireJoinListener implements Listener {
    private final ElementManager elementManager;

    public FireJoinListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) == ElementType.FIRE) {
            // Apply fire resistance effect for Fire element users
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
        }
    }
}
