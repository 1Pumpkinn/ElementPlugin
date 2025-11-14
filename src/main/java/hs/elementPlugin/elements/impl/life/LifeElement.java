package hs.elementPlugin.elements.impl.life;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.impl.life.LifeHealingBeamAbility;
import hs.elementPlugin.elements.abilities.impl.life.LifeRegenAbility;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LifeElement extends BaseElement {

    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    // Fixed: only ONE passive task map
    private final Map<UUID, BukkitTask> passiveTasks = new HashMap<>();

    public LifeElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new LifeRegenAbility(plugin);
        this.ability2 = new LifeHealingBeamAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.LIFE;
    }

    // =====================================================================
    // APPLY UPSIDES — fixed to prevent duplicate tasks
    // =====================================================================
    @Override
    public void applyUpsides(Player player, int upgradeLevel) {

        // Cancel previous task to prevent stacking
        cancelPassiveTask(player);

        // Upside 1: 15 hearts (30 HP)
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) {
                player.setHealth(attr.getBaseValue());
            }
        }

        // Upside 2: crop growth aura (upgrade level 2+)
        if (upgradeLevel >= 2) {
            BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancelPassiveTask(player);
                        return;
                    }

                    int radius = 5;

                    // Force-grow crops within 5x5 around the player
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            for (int dy = -1; dy <= 1; dy++) {
                                Block block = player.getLocation().clone().add(dx, dy, dz).getBlock();
                                growIfCrop(block);
                            }
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds

            // Store the running task
            passiveTasks.put(player.getUniqueId(), task);
        }
    }

    // =====================================================================
    // Crop Growth Helper
    // =====================================================================
    private boolean growIfCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) {
                ageable.setAge(ageable.getMaximumAge());
                block.setBlockData(ageable);
                return true;
            }
        }
        return false;
    }

    // =====================================================================
    // Cancel passive task
    // =====================================================================
    private void cancelPassiveTask(Player player) {
        BukkitTask task = passiveTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

    // =====================================================================
    // Abilities
    // =====================================================================
    @Override
    protected boolean executeAbility1(ElementContext context) {
        return ability1.execute(context);
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        return ability2.execute(context);
    }

    // =====================================================================
    // Clearing Effects
    // =====================================================================
    @Override
    public void clearEffects(Player player) {

        // Stop aura
        cancelPassiveTask(player);

        // Reset health to normal
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    // =====================================================================
    // Display
    // =====================================================================
    @Override
    public String getDisplayName() {
        return ChatColor.GREEN + "Life";
    }

    @Override
    public String getDescription() {
        return "Masters of healing and growth. Life users have increased health and can heal allies.";
    }

    @Override
    public String getAbility1Name() {
        return ability1.getName();
    }

    @Override
    public String getAbility1Description() {
        return ability1.getDescription();
    }

    @Override
    public String getAbility2Name() {
        return ability2.getName();
    }

    @Override
    public String getAbility2Description() {
        return ability2.getDescription();
    }
}
