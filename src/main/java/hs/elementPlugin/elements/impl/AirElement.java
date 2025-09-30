package hs.elementPlugin.elements.impl;

import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
    public boolean ability1(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust) {
        if (upgradeLevel < 1) {
            player.sendMessage(ChatColor.RED + "You need Upgrade I to use this ability.");
            return false;
        }
        int cost = 50;
        if (!mana.spend(player, cost)) {
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
            w.spawnParticle(Particle.CLOUD, center.clone().add(x, 0.2, z), 2, 0.0, 0.0, 0.0, 0.0);
        }

        for (LivingEntity e : player.getLocation().getNearbyLivingEntities(radius)) {
            if (e instanceof Player other) {
                if (other.equals(player)) continue;
                if (trust.isTrusted(player.getUniqueId(), other.getUniqueId())) continue; // don't affect trusted
            }
            Vector push = e.getLocation().toVector().subtract(center.toVector()).normalize().multiply(1.2).setY(0.6);
            e.setVelocity(push);
        }
        w.playSound(center, Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
        return true;
    }

    // Ability 2

    @Override
    public boolean ability2(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust) {
        if (upgradeLevel < 2) {
            player.sendMessage(ChatColor.RED + "You need Upgrade II to use this ability.");
            return false;
        }
        int cost = 100;
        if (!mana.spend(player, cost)) {
            player.sendMessage(ChatColor.RED + "Not enough mana (" + cost + ")");
            return false;
        }
        // For 5 seconds, make weapon feel like a mace with added strength and airtime control
        int duration = 5 * 20;
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 1, true, true, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration, 2, true, true, true));
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_MACE_SMASH_GROUND, 1f, 1f);
        return true;
    }
}