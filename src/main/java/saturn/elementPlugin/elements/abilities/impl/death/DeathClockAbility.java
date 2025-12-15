package saturn.elementPlugin.elements.abilities.impl.death;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Death Clock - Next hit applies blindness, weakness, and wither for 3 seconds
 */
public class DeathClockAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public static final String META_DEATH_CLOCK_ACTIVE = "death_clock_active";

    public DeathClockAbility(ElementPlugin plugin) {
        super("death_clock", 75, 10, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        player.setMetadata(META_DEATH_CLOCK_ACTIVE, new FixedMetadataValue(plugin, true));

        player.sendMessage(ChatColor.DARK_PURPLE + "Death Clock activated! Your next hit will curse the target.");
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);

        player.getWorld().spawnParticle(
                org.bukkit.Particle.SMOKE,
                player.getLocation().add(0, 1, 0),
                20, 0.5, 0.5, 0.5, 0.05
        );

        setActive(player, true);
        return true;
    }

    /**
     * Apply Death Clock effects to a target
     * Called from combat listener when player hits an enemy
     */
    public static void applyEffects(ElementPlugin plugin, Player attacker, LivingEntity target) {
        int duration = 200;

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.DARKNESS, duration, 0, false, true, true
        ));
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS, duration, 0, false, true, true
        ));
        target.addPotionEffect(new PotionEffect(
                PotionEffectType.WITHER, duration, 0, false, true, true
        ));

        // Visual effects
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_WITHER_HURT, 1.0f, 1.2f);
        target.getWorld().spawnParticle(
                org.bukkit.Particle.SMOKE,
                target.getLocation().add(0, 1, 0),
                30, 0.3, 0.5, 0.3, 0.05
        );
        // Remove the active marker
        attacker.removeMetadata(META_DEATH_CLOCK_ACTIVE, plugin);
    }

    @Override
    public String getName() {
        return ChatColor.DARK_PURPLE + "Death Clock";
    }

    @Override
    public String getDescription() {
        return "Your next hit curses the target with blindness, weakness, and wither for 3 seconds.";
    }
}