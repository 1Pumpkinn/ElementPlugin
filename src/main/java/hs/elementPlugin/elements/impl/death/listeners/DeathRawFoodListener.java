package hs.elementPlugin.elements.impl.death.listeners;

import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DeathRawFoodListener implements Listener {
    private final ElementManager elementManager;

    public DeathRawFoodListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData pd = elementManager.data(player.getUniqueId());
        if (pd == null || pd.getCurrentElement() != ElementType.DEATH) return;
        Material food = event.getItem().getType();
        if (isRawOrUndeadFood(food)) {
            // Apply regular golden apple effects (not enchanted)
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1)); // 5s regen II
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 0)); // 2 min absorption I
        }
    }

    private boolean isRawOrUndeadFood(Material food) {
        // Add more as needed
        return food == Material.ROTTEN_FLESH || food == Material.CHICKEN || food == Material.BEEF || food == Material.PORKCHOP || food == Material.MUTTON || food == Material.RABBIT || food == Material.COD || food == Material.SALMON;
    }
}
