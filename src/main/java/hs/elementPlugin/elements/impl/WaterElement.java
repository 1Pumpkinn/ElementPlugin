package hs.elementPlugin.elements.impl;

import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class WaterElement implements Element {
    @Override
    public ElementType getType() { return ElementType.WATER; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Infinite Conduit Power I
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    public boolean ability1(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 1) {
            player.sendMessage(ChatColor.RED + "You need Upgrade I to use this ability.");
            return false;
        }
        int cost = config.getAbility1Cost(ElementType.WATER);
        if (!mana.spend(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }
        // Ray trace a target up to 20 blocks
        RayTraceResult rt = player.rayTraceEntities(20);
        if (rt == null || rt.getHitEntity() == null || !(rt.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage(ChatColor.YELLOW + "No target in sight.");
            return false;
        }
        if (target instanceof Player victim) {
            if (trust.isTrusted(player.getUniqueId(), victim.getUniqueId())) {
                player.sendMessage(ChatColor.YELLOW + "Target is trusted.");
                return false;
            }
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
        }.runTaskTimer(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), 0L, 2L);
        return true;
    }

    @Override
    public boolean ability2(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 2) {
            player.sendMessage(ChatColor.RED + "You need Upgrade II to use this ability.");
            return false;
        }
        int cost = config.getAbility2Cost(ElementType.WATER);
        if (!mana.spend(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }
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
                    if (le instanceof Player pv && trust.isTrusted(player.getUniqueId(), pv.getUniqueId())) {
                        // skip impact on trusted
                    } else {
                        // Half a heart per second = 0.5 damage per second
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
        }.runTaskTimer(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), 0L, 20L);
        return true;
    }
}