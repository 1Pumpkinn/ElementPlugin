package hs.elementPlugin.elements.impl.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.impl.death.DeathWitherSkullAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathSummonUndeadAbility;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class DeathElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> passiveTasks = new java.util.HashMap<>();

    public DeathElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new DeathWitherSkullAbility(plugin);
        this.ability2 = new DeathSummonUndeadAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.DEATH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Cancel any existing passive task for this player
        cancelPassiveTask(player);
        
        // Upside 1: Any raw or undead foods act as golden apples (handled in a listener)
        // Upside 2: Nearby enemies get hunger 1 in a 5x5 radius (if upgradeLevel >= 2)
        if (upgradeLevel >= 2) {
            // Start a repeating task to continuously apply hunger to nearby enemies
            org.bukkit.scheduler.BukkitTask task = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        passiveTasks.remove(player.getUniqueId());
                        return;
                    }
                    
                    // Apply hunger to nearby players and mobs in 5x5 radius
                    int radius = 5;
                    for (Player other : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
                        if (!other.equals(player)) {
                            other.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 40, 0, true, true, true)); // 2 seconds
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L); // Every second
            
            // Store the task reference
            passiveTasks.put(player.getUniqueId(), task);
        }
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
        return ChatColor.DARK_PURPLE + "Death";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Master of decay and the undead. Death users can corrupt food and summon wither powers.";
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
