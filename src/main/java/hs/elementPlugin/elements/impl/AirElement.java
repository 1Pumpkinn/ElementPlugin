package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirElement extends BaseElement {

    public AirElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.AIR;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Permanent speed 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
    }



    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        double radius = 6.0;
        World w = player.getWorld();
        Location center = player.getLocation();

        // Animated particle ring that shoots outward
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                double currentRadius = 1.5 + (tick * 0.8);
                if (currentRadius > 8.0) {
                    cancel();
                    return;
                }

                // Spawn particles in a ring
                for (int i = 0; i < 360; i += 10) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * currentRadius;
                    double z = Math.sin(rad) * currentRadius;

                    int count = Math.max(1, 3 - tick/2);
                    w.spawnParticle(Particle.CLOUD, center.clone().add(x, 0.2, z), count, 0.0, 0.0, 0.0, 0.0);
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);

        // Launch nearby entities
        for (LivingEntity e : player.getLocation().getNearbyLivingEntities(radius)) {
            if (!isValidTarget(context, e)) continue;
            Vector push = e.getLocation().toVector().subtract(center.toVector()).normalize().multiply(2.25).setY(1.5);
            e.setVelocity(push);
        }
        w.playSound(center, Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        int duration = 15 * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 2, true, true, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        player.sendMessage(ChatColor.GREEN + "Mace empowerment active for 15s!");
        return true;
    }
}