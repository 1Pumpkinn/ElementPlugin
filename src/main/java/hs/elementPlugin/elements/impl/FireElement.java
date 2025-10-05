package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
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
        
        // Check for 50 mana cost
        if (!context.getManaManager().hasMana(player, 50)) {
            player.sendMessage(ChatColor.RED + "Not enough mana! (50 required)");
            return false;
        }
        
        // Consume mana
        context.getManaManager().spend(player, 50);
        
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
        
        // Mark ability as active to prevent stacking
        setAbility1Active(player, true);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) { 
                    setAbility1Active(player, false);
                    cancel(); 
                    return; 
                }
                
                Location playerLoc = player.getLocation().add(0, 1.2, 0); // Chest level
                Vector dir = player.getLocation().getDirection().normalize();

                // Start cone 1.5 blocks in front of player at chest level
                Vector startOffset = dir.clone().multiply(1.5);
                Location startLoc = playerLoc.clone().add(startOffset);

                // Create cone shape that widens as it goes out
                for (double d = 0; d <= 6; d += 0.4) {
                    Vector step = dir.clone().multiply(d);
                    Location centerLoc = startLoc.clone().add(step);

                    // Cone radius increases with distance
                    double coneRadius = d * 0.25; // Starts at 0, ends at 1.5 blocks wide

                    // Spawn particles in a circle at this distance
                    int particleCount = Math.max(1, (int)(coneRadius * 8));
                    for (int i = 0; i < particleCount; i++) {
                        double angle = (Math.PI * 2 * i) / particleCount;
                        Vector perpendicular = getPerpendicular(dir);
                        Vector rotated = rotateAroundAxis(perpendicular, dir, angle).multiply(coneRadius);
                        Location particleLoc = centerLoc.clone().add(rotated);
                        player.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0.05, 0.05, 0.05, 0.01);
                    }

                    // Damage and knockback entities in cone
                    for (LivingEntity le : centerLoc.getNearbyLivingEntities(coneRadius + 0.5)) {
                        if (!isValidTarget(context, le)) continue;
                        le.setFireTicks(40);
                        
                        // Apply knockback effect
                        Vector knockback = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
                        knockback.setY(0.3); // Add upward component
                        knockback = knockback.multiply(1.2); // Knockback strength
                        le.setVelocity(knockback);
                        
                        // True damage that bypasses armor (0.5 hearts per second)
                        double currentHealth = le.getHealth();
                        double newHealth = Math.max(0, currentHealth - 0.5);
                        le.setHealth(newHealth);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for continuous fire breath
        
        // End the ability after 5 seconds and mark as inactive
        new BukkitRunnable() {
            @Override
            public void run() {
                setAbility1Active(player, false);
            }
        }.runTaskLater(plugin, 100L); // 5 seconds = 100 ticks
        
        return true;
    }

    // Helper method to get a perpendicular vector
    private Vector getPerpendicular(Vector v) {
        if (Math.abs(v.getX()) < 0.9) {
            return new Vector(1, 0, 0).crossProduct(v).normalize();
        } else {
            return new Vector(0, 1, 0).crossProduct(v).normalize();
        }
    }

    // Helper method to rotate a vector around an axis
    private Vector rotateAroundAxis(Vector v, Vector axis, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double dot = v.dot(axis);

        return v.clone().multiply(cos)
                .add(axis.clone().crossProduct(v).multiply(sin))
                .add(axis.clone().multiply(dot * (1 - cos)));
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        // Spawn 3 friendly blazes with 20 hearts
        for (int i = 0; i < 3; i++) {
            // Calculate spawn location in front of player, above ground
            Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2.0));
            // Ensure spawning above ground level
            spawnLoc.setY(Math.max(spawnLoc.getY(), player.getWorld().getHighestBlockYAt(spawnLoc) + 2));
            
            Blaze blaze = player.getWorld().spawn(spawnLoc, Blaze.class);
            var attr = blaze.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null) attr.setBaseValue(40.0);
            blaze.setHealth(40.0);
            blaze.setMetadata(META_FRIENDLY_BLAZE_OWNER, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        }
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1f, 0.8f);
        return true;
    }
}