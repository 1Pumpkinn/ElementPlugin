package hs.elementPlugin.elements.impl.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;
    private final java.util.Map<java.util.UUID, org.bukkit.scheduler.BukkitTask> passiveTasks = new java.util.HashMap<>();

    public DeathElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new DeathClockAbility(plugin);
        this.ability2 = new DeathSlashAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.DEATH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Cancel any existing passive task for this player
        cancelPassiveTask(player);

        if (upgradeLevel >= 2) {

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

        // Clear ability metadata
        player.removeMetadata(DeathClockAbility.META_DEATH_CLOCK_ACTIVE, plugin);
        player.removeMetadata(DeathSlashAbility.META_SLASH_ACTIVE, plugin);
        player.removeMetadata(DeathSlashAbility.META_BLEEDING, plugin);

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
        return ChatColor.GRAY + "Master of curses and afflictions. Death users can curse enemies and cause bleeding.";
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