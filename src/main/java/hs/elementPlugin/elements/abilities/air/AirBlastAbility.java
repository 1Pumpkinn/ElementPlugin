package hs.elementPlugin.elements.abilities.air;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirBlastAbility extends BaseAbility {
    private final hs.elementPlugin.ElementPlugin plugin;

    public AirBlastAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("air_blast", 20, 8, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        double radius = 6.0;
        World w = player.getWorld();
        Location center = player.getLocation();

        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                double currentRadius = 1.5 + (tick * 0.8);
                if (currentRadius > 8.0) {
                    cancel();
                    return;
                }

                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = center.getX() + currentRadius * Math.cos(rad);
                    double z = center.getZ() + currentRadius * Math.sin(rad);
                    Location particleLoc = new Location(w, x, center.getY() + 1, z);
                    w.spawnParticle(Particle.CLOUD, particleLoc, 1, 0.1, 0.1, 0.1, 0.01);
                }

                for (LivingEntity entity : center.getNearbyLivingEntities(currentRadius)) {
                    if (entity.equals(player)) continue;
                    if (!AirBlastAbility.this.isValidTarget(context, entity)) continue;

                    Vector direction = entity.getLocation().toVector().subtract(center.toVector()).normalize();
                    direction.setY(0.3);
                    entity.setVelocity(direction.multiply(1.5));
                    w.spawnParticle(Particle.CLOUD, entity.getLocation(), 10, 0.5, 0.5, 0.5, 0.1);
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 3L);

        w.playSound(center, Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.5f);
        player.sendMessage(ChatColor.WHITE + "Air Blast unleashed!");
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