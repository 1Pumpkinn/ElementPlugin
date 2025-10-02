package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class WaterElement extends BaseElement {

    public WaterElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.WATER; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Infinite Conduit Power I
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();

        // Ray trace a target up to 20 blocks
        RayTraceResult rt = player.rayTraceEntities(20);
        if (rt == null || rt.getHitEntity() == null || !(rt.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage(ChatColor.YELLOW + "No target in sight.");
            return false;
        }

        if (!isValidTarget(context, target)) {
            player.sendMessage(ChatColor.YELLOW + "Target is trusted.");
            return false;
        }

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, 1f, 1f);
        final double startY = target.getLocation().getY();

        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (target.isDead() || !target.isValid()) { cancel(); return; }
                Location loc = target.getLocation();
                target.setVelocity(new Vector(0, 1.8, 0));
                target.getWorld().spawnParticle(Particle.SPLASH, loc.getX(), loc.getY() - 0.01, loc.getZ(), 10, 0.2, 0.0, 0.2, 0.01);
                ticks++;
                if (loc.getY() - startY >= 35 || ticks >= 25) {
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();

        player.sendMessage(ChatColor.AQUA + "Water beam active for 10s...");
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.2f);

        new BukkitRunnable() {
            int cycles = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Vector dir = player.getLocation().getDirection().normalize();
                RayTraceResult r = player.getWorld().rayTraceEntities(player.getEyeLocation(), dir, 20.0,
                        entity -> entity instanceof LivingEntity && !entity.equals(player));
                if (r != null && r.getHitEntity() instanceof LivingEntity le) {
                    if (isValidTarget(context, le)) {
                        le.damage(0.5, player);
                        Location hit = r.getHitPosition().toLocation(player.getWorld());
                        player.getWorld().spawnParticle(Particle.DRIPPING_WATER, hit, 5, 0.1, 0.1, 0.1, 0.01);
                    }
                }
                // Draw beam particles along the path
                Location eye = player.getEyeLocation();
                for (double d = 0; d <= 20; d += 0.5) {
                    Location pt = eye.clone().add(dir.clone().multiply(d));
                    player.getWorld().spawnParticle(Particle.BUBBLE, pt, 1, 0, 0, 0, 0);
                }
                cycles++;
                if (cycles >= 10) cancel();
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return true;
    }
}