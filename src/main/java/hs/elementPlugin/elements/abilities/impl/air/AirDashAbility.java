package hs.elementPlugin.elements.abilities.impl.air;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirDashAbility extends BaseAbility {
    private final hs.elementPlugin.ElementPlugin plugin;

    public AirDashAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("air_dash", 75, 5, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Vector direction = player.getLocation().getDirection();
        direction.setY(Math.max(direction.getY(), 0.5));
        player.setVelocity(direction.multiply(2.5));

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20 || !player.isOnline()) {
                    cancel();
                    return;
                }

				Location loc = player.getLocation();
				player.getWorld().spawnParticle(Particle.CLOUD, loc, 5, 0.3, 0.3, 0.3, 0.05, null, true);

                if (ticks % 5 == 0) {
                    for (LivingEntity entity : loc.getNearbyLivingEntities(3.0)) {
                        if (entity.equals(player)) continue;
                        if (!AirDashAbility.this.isValidTarget(context, entity)) continue;

                        Vector knockback = entity.getLocation().toVector().subtract(loc.toVector()).normalize();
                        knockback.setY(0.2);
						entity.setVelocity(knockback.multiply(1.0));
						entity.getWorld().spawnParticle(Particle.CLOUD, entity.getLocation(), 10, 0.3, 0.3, 0.3, 0.05, null, true);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.5f);
        return true;
    }

    @Override
    public String getName() {
        return ChatColor.WHITE + "Air Dash";
    }

    @Override
    public String getDescription() {
        return "Dash forward with incredible speed, pushing away any enemies in your path.";
    }
}