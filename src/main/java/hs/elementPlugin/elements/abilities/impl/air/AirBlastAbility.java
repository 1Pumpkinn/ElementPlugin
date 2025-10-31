package hs.elementPlugin.elements.abilities.impl.air;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirBlastAbility extends BaseAbility {
    private final hs.elementPlugin.ElementPlugin plugin;

    public AirBlastAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("air_blast", 50, 8, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        ManaManager mana = context.getManaManager();
        TrustManager trust = context.getTrustManager();
        int cost = 20;
        
        if (!mana.hasMana(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }
        // Launch all nearby players away, with particles

        double radius = 6.0;
        World w = player.getWorld();
        Location center = player.getLocation();

        // Particle ring
		for (int i = 0; i < 360; i += 10) {
            double rad = Math.toRadians(i);
            double x = Math.cos(rad) * 1.5;
            double z = Math.sin(rad) * 1.5;
			w.spawnParticle(Particle.CLOUD, center.clone().add(x, 0.2, z), 2, 0.0, 0.0, 0.0, 0.0, null, true);
        }
        // Animated particle ring that shoots outward
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                double currentRadius = 1.5 + (tick * 0.8); // Expands outward
                if (currentRadius > 8.0) {
                    cancel();
                    return;
                }

                // Spawn particles in a ring
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    // Particles shrink as they move outward
					int count = Math.max(1, 3 - tick/2);
					w.spawnParticle(Particle.CLOUD, center.clone().add(x, 0.2, z), count, 0.0, 0.0, 0.0, 0.0, null, true);
                }
                tick++;
            }
        }.runTaskTimer(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), 0L, 1L);

        // Launch nearby entities
        for (LivingEntity e : player.getLocation().getNearbyLivingEntities(radius)) {
            if (e instanceof Player other) {
                if (other.equals(player)) continue;
                if (trust.isTrusted(player.getUniqueId(), other.getUniqueId())) continue; // don't affect trusted
            }
            Vector push = e.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.25).setY(1.5);
            e.setVelocity(push);
        }

        w.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
        return true;
    }

    @Override
    public String getName() {
        return ChatColor.WHITE + "Air Blast";
    }

    @Override
    public String getDescription() {
        return "Create a powerful blast of air that pushes enemies away from you.";
    }
}