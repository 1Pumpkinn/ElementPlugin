package saturn.elementPlugin.elements.impl.life.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class LifeRegenListener implements Listener {
    private final ElementManager elementManager;

    public LifeRegenListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onHealthRegain(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (elementManager.getPlayerElement(player) != ElementType.LIFE) return;
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED ||
            event.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            event.setAmount(event.getAmount() * 1.5); // 50% faster natural regeneration
        }
    }
}
