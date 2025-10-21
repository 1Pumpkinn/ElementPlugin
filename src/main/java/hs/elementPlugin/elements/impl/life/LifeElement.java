package hs.elementPlugin.elements.impl.life;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.impl.life.LifeHealingBeamAbility;
import hs.elementPlugin.elements.abilities.impl.life.LifeRegenAbility;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class LifeElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> passiveTasks = new java.util.HashMap<>();

    public LifeElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new LifeRegenAbility(plugin);
        this.ability2 = new LifeHealingBeamAbility(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.LIFE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Cancel any existing passive task for this player
        cancelPassiveTask(player);
        
        // Upside 1: 15 hearts (30 HP)
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) player.setHealth(attr.getBaseValue());
        }
        
        // Upside 2: Crops within 5x5 radius instantly grow (passive effect)
        if (upgradeLevel >= 2) {
            org.bukkit.scheduler.BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        passiveTasks.remove(player.getUniqueId());
                        return;
                    }
                    
                    // Grow crops in 5x5 around player every 2 seconds
                    int radius = 5;
                    int cropsGrown = 0;
                    for (int dx = -radius; dx <= radius; dx++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            for (int dy = -1; dy <= 1; dy++) { // Check 3 levels vertically
                                Block b = player.getLocation().clone().add(dx, dy, dz).getBlock();
                                if (growIfCrop(b)) {
                                    cropsGrown++;
                                }
                            }
                        }
                    }
                    if (cropsGrown > 0) {
                    }
                }
            }.runTaskTimer(plugin, 0L, 40L); // Every 2 seconds
            
            // Store the task reference
            passiveTasks.put(player.getUniqueId(), task);
        } else {
        }
    }

    private boolean growIfCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) {
                ageable.setAge(ageable.getMaximumAge());
                block.setBlockData(ageable);
                // Debug message to confirm crop growth
                plugin.getLogger().info("Grew crop at " + block.getLocation());
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        return ability1.execute(context);
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        return ability2.execute(context);
    }

    @Override
    public void clearEffects(Player player) {
        // Cancel passive task
        cancelPassiveTask(player);
        
        // Reset max health to default
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) player.setHealth(20.0);
        }
        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }
    
    /**
     * Cancel the passive task for a player
     * @param player The player to cancel the task for
     */
    private void cancelPassiveTask(Player player) {
        org.bukkit.scheduler.BukkitTask task = passiveTasks.remove(player.getUniqueId());
        if (task != null && !task.isCancelled()) {
            task.cancel();
        }
    }

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