package saturn.elementPlugin.elements.impl.water.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterJoinListener implements Listener {
    private final ElementManager elementManager;

    public WaterJoinListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (elementManager.getPlayerElement(player) == ElementType.WATER) {
            // Apply conduit power effect for Water element users
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));

            // Apply underwater mining speed if they have upgrade 2
            var pd = elementManager.data(player.getUniqueId());
            if (pd.getUpgradeLevel(ElementType.WATER) >= 2) {
                var attr = player.getAttribute(Attribute.SUBMERGED_MINING_SPEED);
                if (attr != null) {
                    attr.setBaseValue(1.2); // Slightly faster than on land
                }
            }
        }
    }
}