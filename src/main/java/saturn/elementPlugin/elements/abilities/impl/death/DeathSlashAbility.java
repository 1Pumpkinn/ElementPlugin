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

        bloodBurst(player.getLocation().add(0, 1, 0));
        setActive(player, true);
        return true;
    }

    /**
     * Called when the empowered hit lands
     */
    public static void applyBleeding(ElementPlugin plugin, Player attacker, LivingEntity target) {

        long bleedUntil = System.currentTimeMillis() + 5000L;
        target.setMetadata(META_BLEEDING, new FixedMetadataValue(plugin, bleedUntil));

        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 0.8f);

        DeathSlashAbility ability =
                (DeathSlashAbility) plugin.getAbilityManager().getAbility("death_slash");

        attacker.removeMetadata(META_SLASH_ACTIVE, plugin);
        ability.bloodBurst(target.getLocation().add(0, 1, 0));

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 100;

            @Override
            public void run() {

                // HARD STOP CONDITIONS (includes totem pop)
                if (!target.isValid()
                        || target.isDead()
                        || !target.hasMetadata(META_BLEEDING)
                        || ticks >= maxTicks) {
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

                if (ticks % 20 == 0) {

                    // If invulnerable (totem pop), STOP ENTIRE BLEED
                    if (target.getNoDamageTicks() > 0) {
                        target.removeMetadata(META_BLEEDING, plugin);
                        cancel();
                        return;
                    }

                    double damage = 1.0; // 0.5 hearts
                    double health = target.getHealth();

                    if (health - damage <= 0) {
                        // Use damage API so totem can trigger
                        target.damage(damage, attacker);
                    } else {
                        target.setHealth(health - damage);
                        target.damage(0.0);
                    }

                    ability.bloodBurst(target.getLocation().add(0, 1, 0));
                    target.getWorld().playSound(
                            target.getLocation(),
                            Sound.ENTITY_GENERIC_HURT,
                            0.5f,
                            1.0f
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
        return "Your next hit makes enemies bleed for 0.5 hearts per second for 5 seconds.";
    }
}
