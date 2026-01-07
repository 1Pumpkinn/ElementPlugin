package saturn.elementPlugin.elements.abilities.impl.death;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathSlashAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public static final String META_SLASH_ACTIVE = "death_slash_active";
    public static final String META_BLEEDING = "death_slash_bleeding";
    public static final String META_TRUE_DAMAGE = "TRUE_DAMAGE";

    public DeathSlashAbility(ElementPlugin plugin) {
        super("death_slash", 50, 10, 1);
        this.plugin = plugin;
    }

    private void bloodBurst(Location loc) {
        loc.getWorld().spawnParticle(
                Particle.BLOCK,
                loc,
                80,
                0.6, 0.6, 0.6,
                0.15,
                Material.REDSTONE_BLOCK.createBlockData()
        );
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        player.setMetadata(META_SLASH_ACTIVE, new FixedMetadataValue(plugin, true));
        player.sendMessage(ChatColor.RED + "Slash activated! Your next hit will cause bleeding.");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 0.8f);

        bloodBurst(player.getLocation().add(0, 1, 0));
        setActive(player, true);
        return true;
    }

    public static void applyBleeding(ElementPlugin plugin, Player attacker, LivingEntity target) {

        long bleedEnd = System.currentTimeMillis() + 5000L; // 5 seconds
        target.setMetadata(META_BLEEDING, new FixedMetadataValue(plugin, bleedEnd));

        DeathSlashAbility ability =
                (DeathSlashAbility) plugin.getAbilityManager().getAbility("death_slash");

        attacker.removeMetadata(META_SLASH_ACTIVE, plugin);
        ability.bloodBurst(target.getLocation().add(0, 1, 0));

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) {
                    target.removeMetadata(META_BLEEDING, plugin);
                    cancel();
                    return;
                }

                long end = target.getMetadata(META_BLEEDING).get(0).asLong();
                if (System.currentTimeMillis() >= end) {
                    target.removeMetadata(META_BLEEDING, plugin);
                    cancel();
                    return;
                }

                // Every second (20 ticks) - TRUE DAMAGE: ½ heart per second
                if (ticks % 20 == 0) {
                    // CRITICAL: Set metadata with current timestamp RIGHT BEFORE damage
                    // This ensures TrueDamageListener will process it
                    target.setMetadata(META_TRUE_DAMAGE,
                            new FixedMetadataValue(plugin, System.currentTimeMillis()));

// Only damage if not trusted
    if (saturn.elementPlugin.util.AbilityTrustValidator.canAffectTarget(plugin, attacker, target, false)) {
        target.damage(1.0, attacker);
    }

                    ability.bloodBurst(target.getLocation().add(0, 1, 0));

                    plugin.getLogger().fine("Applied Death Slash bleed damage (true damage) to " +
                            target.getName());
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
        return "Your next hit causes bleeding, dealing ½ heart of true damage per second for 5 seconds.";
    }
}