package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class FireElement extends BaseElement {
    public static final String META_FRIENDLY_BLAZE_OWNER = "fire_friendly_owner";

    public FireElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.FIRE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Infinite Fire Resistance
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
        // Upside 2 is handled elsewhere (auto-smelt)
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { cancel(); return; }
                Location eye = player.getEyeLocation();
                Vector dir = eye.getDirection().normalize();
                // Cone damage and particles
                for (double d = 0; d <= 6; d += 0.5) {
                    Vector step = dir.clone().multiply(d);
                    Location loc = eye.clone().add(step);
                    player.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.01);
                    for (LivingEntity le : loc.getNearbyLivingEntities(1.0)) {
                        if (!isValidTarget(context, le)) continue;
                        le.setFireTicks(40);
                        if (ticks % 10 == 0) le.damage(1.0, player); // ~0.5 heart per second overall
                    }
                }
                ticks += 2; // runs every 2 ticks below
                if (ticks >= 5 * 20) cancel();
            }
        }.runTaskTimer(plugin, 0L, 2L);
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        // Spawn 3 friendly blazes with 20 hearts
        for (int i = 0; i < 3; i++) {
            Blaze blaze = player.getWorld().spawn(player.getLocation().add(player.getLocation().getDirection().multiply(1.5)), Blaze.class);
            var attr = blaze.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(40.0);
            blaze.setHealth(40.0);
            blaze.setMetadata(META_FRIENDLY_BLAZE_OWNER, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
        return true;
    }
}