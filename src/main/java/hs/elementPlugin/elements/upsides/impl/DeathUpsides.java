package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class DeathUpsides extends BaseUpsides {

    private final ElementPlugin plugin;
    private final Map<UUID, BukkitTask> passiveTasks = new HashMap<>();

    public DeathUpsides(ElementManager elementManager) {
        super(elementManager);
        this.plugin = elementManager.getPlugin();
    }

    @Override
    public ElementType getElementType() {
        return ElementType.DEATH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        cancelPassiveTask(player);

        // Upside 1: Permanent Night Vision
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.NIGHT_VISION,
                Integer.MAX_VALUE,
                0,
                true,
                false,
                false
        ));

        // Upside 2: Hunger aura (requires upgrade level 2)
        if (upgradeLevel >= 2) {
            startHungerAura(player);
        }
    }

    private void startHungerAura(Player player) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelPassiveTask(player);
                    return;
                }

                int radius = 5;
                for (Player other : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
                    if (!other.equals(player)) {
                        other.addPotionEffect(new PotionEffect(
                                PotionEffectType.HUNGER, 40, 0, true, true, true
                        ));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);

        passiveTasks.put(player.getUniqueId(), task);
    }

    private void cancelPassiveTask(Player player) {
        BukkitTask task = passiveTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    /**
     * Check if player should have Night Vision
     * @param player The player to check
     * @return true if player should have Night Vision
     */
    public boolean shouldHaveNightVision(Player player) {
        return hasElement(player);
    }

    /**
     * Check if player should apply hunger aura (Upside 2)
     * @param player The player to check
     * @return true if hunger aura should be active
     */
    public boolean shouldApplyHungerAura(Player player) {
        return hasElement(player) && getUpgradeLevel(player) >= 2;
    }
}