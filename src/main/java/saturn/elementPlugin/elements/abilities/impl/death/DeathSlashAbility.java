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

        // activation blood
        bloodBurst(player.getLocation().add(0, 1, 0));

        setActive(player, true);
        return true;
    }

    public static void applyBleeding(ElementPlugin plugin, Player attacker, LivingEntity target) {

        long bleedUntil = System.currentTimeMillis() + 5000L;
        target.setMetadata(META_BLEEDING, new FixedMetadataValue(plugin, bleedUntil));

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);

        // Get THIS ability instance
        DeathSlashAbility ability =
                (DeathSlashAbility) plugin.getAbilityManager().getAbility("death_slash");

        // Initial blood burst
        ability.bloodBurst(target.getLocation().add(0, 1, 0));
        attacker.removeMetadata(META_SLASH_ACTIVE, plugin);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;

            @Override
            public void run() {
                if (!target.isValid() || target.isDead() || ticks >= maxTicks) {
                    target.removeMetadata(META_BLEEDING, plugin);
                    cancel();
                    return;
                }

                if (!target.hasMetadata(META_BLEEDING)) {
                    cancel();
                    return;
                }

                long end = target.getMetadata(META_BLEEDING).get(0).asLong();
                if (System.currentTimeMillis() >= end) {
                    target.removeMetadata(META_BLEEDING, plugin);
                    cancel();
                    return;
                }

                if (ticks % 20 == 0) {
                    double currentHealth = target.getHealth();
                    double damageAmount = 1.0; // 0.5 hearts

                    // CRITICAL FIX: If this damage would kill the target, use the damage API
                    // This allows totems to trigger properly
                    if (currentHealth - damageAmount <= 0.0) {
                        // Use damage API to allow totem to trigger
                        target.damage(damageAmount, attacker);

                        // If they survived (totem popped), continue bleeding
                        if (target.isValid() && !target.isDead()) {
                            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.5f, 1.0f);
                            ability.bloodBurst(target.getLocation().add(0, 1, 0));
                        }
                    } else {
                        // Safe to use true damage - won't kill them
                        target.setHealth(currentHealth - damageAmount);

                        // Play hurt sound and animation
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_GENERIC_HURT, 0.5f, 1.0f);
                        target.damage(0.0); // Trigger hurt animation without actual damage

                        // BLEED TICK BLOOD
                        ability.bloodBurst(target.getLocation().add(0, 1, 0));
                    }
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