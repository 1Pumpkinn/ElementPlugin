package hs.elementPlugin.elements.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.Set;
import java.util.UUID;

public class DeathElementListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public DeathElementListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    // Upside 1: Raw/undead foods act as golden apples
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

    // Upside 2: Passive hunger effect (should be called periodically, e.g., on move or with a repeating task)
    public void applyPassiveHunger(Player player) {
        PlayerData pd = elementManager.data(player.getUniqueId());
        if (pd == null || pd.getCurrentElement() != ElementType.DEATH || pd.getUpgradeLevel(ElementType.DEATH) < 2) return;
        for (Player target : player.getWorld().getPlayers()) {
            if (!target.equals(player) && target.getLocation().distance(player.getLocation()) <= 5) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, true, true, true)); // 3s
            }
        }
    }
}
