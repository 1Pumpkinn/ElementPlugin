package hs.elementPlugin.elements.upsides.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.impl.death.listeners.DeathCombatListener;
import hs.elementPlugin.elements.upsides.BaseUpsides;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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


        // Upside 2: Death Wither Effect (requires upgrade level 2)
        if (upgradeLevel >= 2) {
        }
    }

    private void cancelPassiveTask(Player player) {
        BukkitTask task = passiveTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }
}