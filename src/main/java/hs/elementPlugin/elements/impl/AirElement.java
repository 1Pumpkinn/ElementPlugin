package hs.elementPlugin.elements.impl;

import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class AirElement implements Element {
    @Override
    public ElementType getType() {
        return ElementType.AIR;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Permanent speed 1
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
    }

    // Ability 1

    @Override
    public boolean ability1(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 1) {
            player.sendMessage(ChatColor.RED + "You need Upgrade I to use this ability.");
            return false;
        }
        int cost = config.getAbility1Cost(ElementType.AIR);
        if (!mana.spend(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }

        double radius = 6.0;
        World w = player.getWorld();
        Location center = player.getLocation();

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
                    w.spawnParticle(Particle.CLOUD, center.clone().add(x, 0.2, z), count, 0.0, 0.0, 0.0, 0.0);
                }
                tick++;
            }
        }.runTaskTimer(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), 0L, 1L);

        // Launch nearby entities
        for (LivingEntity e : player.getLocation().getNearbyLivingEntities(radius)) {
            if (e instanceof Player other) {
                if (other.equals(player)) continue;
                if (trust.isTrusted(player.getUniqueId(), other.getUniqueId())) continue;
            }
            Vector push = e.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2).setY(0.6);
            e.setVelocity(push);
        }
        w.playSound(center, Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
        return true;
    }

    // Ability 2

    @Override
    public boolean ability2(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 2) {
            player.sendMessage(ChatColor.RED + "You need Upgrade II to use this ability.");
            return false;
        }
        int cost = config.getAbility2Cost(ElementType.AIR);
        if (!mana.spend(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }
        // For 15 seconds, make weapon feel like a mace with added strength and airtime control
        int duration = 15 * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 2, true, true, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
        player.sendMessage(ChatColor.GREEN + "Mace empowerment active for 15s!");
        return true;
    }
}