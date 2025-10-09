package hs.elementPlugin.elements.impl.death.listeners;

import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DeathPassiveHunger {
    private final ElementManager elementManager;

    public DeathPassiveHunger(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

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
