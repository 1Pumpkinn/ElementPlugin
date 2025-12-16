package saturn.elementPlugin.elements.impl.life.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Life Upside 1: Slower hunger drain (50% slower)
 * This makes food last twice as long
 */
public class LifeHungerListener implements Listener {
    private final ElementManager elementManager;

    public LifeHungerListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Check if player has Life element
        var pd = elementManager.data(player.getUniqueId());
        if (pd.getCurrentElement() != ElementType.LIFE) return;

        int oldLevel = player.getFoodLevel();
        int newLevel = event.getFoodLevel();

        // Only apply to hunger LOSS (not gain from eating)
        if (newLevel < oldLevel) {
            // 15% chance to cancel hunger loss = 15% slower drain
            if (Math.random() <0.15) {
                event.setCancelled(true);
            }
        }
    }
}