package saturn.elementPlugin.elements.impl.metal.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MetalJoinListener implements Listener {
    private final ElementManager elementManager;

    public MetalJoinListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) == ElementType.METAL) {
            // Apply haste effect for Metal element users
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));
        }
    }
}