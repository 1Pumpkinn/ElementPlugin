package hs.elementPlugin.elements.abilities.impl.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
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
        super("death_clock", 50, 10, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Mark player as having Death Clock active
        player.setMetadata(META_DEATH_CLOCK_ACTIVE, new FixedMetadataValue(plugin, true));

        // Visual and audio feedback
        player.sendMessage(ChatColor.DARK_PURPLE + "Death Clock activated! Your next hit will curse the target.");
        player.sendMessage(ChatColor.GRAY + "[DEBUG] Metadata set: " + player.hasMetadata(META_DEATH_CLOCK_ACTIVE));
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1.0f, 0.8f);

        setActive(player, true);
        return true;
    }

    /**
     * Apply Death Clock effects to a target
     * Called from combat listener when player hits an enemy
     */
    public static void applyEffects(ElementPlugin plugin, Player attacker, LivingEntity target) {
        // Apply blindness, weakness, and wither for 3 seconds
        int duration = 60; // 3 seconds = 60 ticks

        target.addPotionEffect(new PotionEffect(
                PotionEffectType.BLINDNESS, duration, 0, false, true, true
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

        // Feedback to attacker
        attacker.sendMessage(ChatColor.DARK_PURPLE + "Death Clock curse applied!");
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