package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;

import org.bukkit.Material;
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
        this.plugin = elementManager.getPlugin();  // ✅ FIX: now plugin is available
    }

    @Override
    public ElementType getElementType() {
        return ElementType.DEATH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        cancelPassiveTask(player);

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
        }.runTaskTimer(plugin, 0L, 20L); // ✅ fixed — plugin now resolves properly

        passiveTasks.put(player.getUniqueId(), task);
    }

    private void cancelPassiveTask(Player player) {
        BukkitTask task = passiveTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    public boolean shouldActAsGoldenApple(Player player, Material foodType) {
        if (!hasElement(player)) return false;
        return isRawOrUndeadFood(foodType);
    }

    public void applyGoldenAppleEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 0));
    }

    private boolean isRawOrUndeadFood(Material food) {
        return switch (food) {
            case ROTTEN_FLESH, CHICKEN, BEEF, PORKCHOP, MUTTON, RABBIT, COD, SALMON -> true;
            default -> false;
        };
    }
}
