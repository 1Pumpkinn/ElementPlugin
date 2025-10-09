package hs.elementPlugin.elements.impl.life.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LifeJoinListener implements Listener {
    private final ElementManager elementManager;

    public LifeJoinListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) == ElementType.LIFE) {
            // Apply regeneration effect for Life element users
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false));
        }
    }
}
