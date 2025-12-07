package hs.elementPlugin.elements.abilities.impl.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.abilities.BaseAbility;
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
        super("death_slash", 75, 10, 2);
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
        attacker.sendMessage(ChatColor.RED + "Slash applied! Target is bleeding.");

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
                    // TRUE DAMAGE with knockback (1.0 damage = 0.5 hearts)
                    // Store absorption hearts to restore them (true damage ignores absorption)
                    double absorption = target.getAbsorptionAmount();

                    // Deal damage (bypasses armor but triggers knockback)
                    target.damage(1.0, attacker);

                    // Restore absorption hearts (optional - remove if you want true damage to also bypass absorption)
                    if (absorption > 0.0) {
                        target.setAbsorptionAmount(absorption);
                    }

                    // BLEED TICK BLOOD
                    ability.bloodBurst(target.getLocation().add(0, 1, 0));
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