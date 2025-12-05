package hs.elementPlugin.elements.abilities.impl.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Slash - Next hit makes enemies bleed, dealing 0.5 hearts per second for 5 seconds
 */
public class DeathSlashAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public static final String META_SLASH_ACTIVE = "death_slash_active";
    public static final String META_BLEEDING = "death_slash_bleeding";

    public DeathSlashAbility(ElementPlugin plugin) {
        super("death_slash", 75, 10, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Mark player as having Slash active
        player.setMetadata(META_SLASH_ACTIVE, new FixedMetadataValue(plugin, true));

        // Visual and audio feedback
        player.sendMessage(ChatColor.RED + "Slash activated! Your next hit will cause bleeding.");
        player.sendMessage(ChatColor.GRAY + "[DEBUG] Metadata set: " + player.hasMetadata(META_SLASH_ACTIVE));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        setActive(player, true);
        return true;
    }

    /**
     * Apply bleeding effect to a target
     * Called from combat listener when player hits an enemy
     */
    public static void applyBleeding(ElementPlugin plugin, Player attacker, LivingEntity target) {
        // Mark target as bleeding
        long bleedUntil = System.currentTimeMillis() + 5000L; // 5 seconds
        target.setMetadata(META_BLEEDING, new FixedMetadataValue(plugin, bleedUntil));

        // Visual effect on hit
        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);
        target.getWorld().spawnParticle(
                Particle.BLOCK,
                target.getLocation().add(0, 1, 0),
                20, 0.3, 0.5, 0.3, 0.1,
                org.bukkit.Material.REDSTONE_BLOCK.createBlockData()
        );

        // Remove the active marker
        attacker.removeMetadata(META_SLASH_ACTIVE, plugin);

        // Feedback to attacker
        attacker.sendMessage(ChatColor.RED + "Slash applied! Target is bleeding.");

        // Start bleeding damage task
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100; // 5 seconds = 100 ticks

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || ticks >= maxTicks) {
                    target.removeMetadata(META_BLEEDING, plugin);
                    cancel();
                    return;
                }

                // Check if bleeding is still active
                if (!target.hasMetadata(META_BLEEDING)) {
                    cancel();
                    return;
                }

                long bleedEndTime = target.getMetadata(META_BLEEDING).get(0).asLong();
                if (System.currentTimeMillis() >= bleedEndTime) {
                    target.removeMetadata(META_BLEEDING, plugin);
                    cancel();
                    return;
                }

                // Deal 0.5 hearts (1.0 damage) per second = every 20 ticks
                if (ticks % 20 == 0) {
                    target.damage(1.0, attacker);

                    // Blood particles
                    target.getWorld().spawnParticle(
                            Particle.BLOCK,
                            target.getLocation().add(0, 1, 0),
                            5, 0.2, 0.3, 0.2, 0.05,
                            org.bukkit.Material.REDSTONE_BLOCK.createBlockData()
                    );
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Slash";
    }

    @Override
    public String getDescription() {
        return "Your next hit makes enemies bleed, dealing 0.5 hearts per second for 5 seconds.";
    }
}