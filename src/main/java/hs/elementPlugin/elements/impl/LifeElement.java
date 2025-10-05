package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class LifeElement extends BaseElement {

    public LifeElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.LIFE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: 15 hearts (30 HP)
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) player.setHealth(attr.getBaseValue());
        }
        
        // Upside 2: Crops within 5x5 radius instantly grow (passive effect)
        if (upgradeLevel >= 2) {
            // This is a passive effect that triggers automatically
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }
                    
                    // Grow crops in 5x5 around player every 5 seconds
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            Block b = player.getLocation().add(dx, 0, dz).getBlock();
                            growIfCrop(b);
                            growIfCrop(b.getRelative(0, 1, 0));
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 100L); // Every 5 seconds
        }
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        
        int radius = 5;
        
        // Show radius with red dust particles (like redstone) that follow the player
        new BukkitRunnable() {
            int tick = 0;
            @Override
            public void run() {
                if (!player.isOnline() || tick >= 60) { // Show for 3 seconds (60 ticks)
                    cancel();
                    return;
                }
                
                // Create a circle of particles at ground level that follows the player
                Location currentCenter = player.getLocation();
                for (int i = 0; i < 360; i += 15) {
                    double rad = Math.toRadians(i);
                    double x = Math.cos(rad) * radius;
                    double z = Math.sin(rad) * radius;
                    
                    Location particleLoc = currentCenter.clone().add(x, 0.5, z);
                    // Ensure particles are visible above ground
                    while (particleLoc.getBlock().getType().isSolid() && particleLoc.getY() < currentCenter.getY() + 3) {
                        particleLoc.add(0, 1, 0);
                    }
                    
                    player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(org.bukkit.Color.RED, 1.0f));
                }
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        
        // Give regeneration 2 to player and trusted people in 5x5 radius for 10 seconds
        for (Player other : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
            if (other.equals(player) || context.getTrustManager().isTrusted(player.getUniqueId(), other.getUniqueId())) {
                other.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, true, true, true));
            }
        }
        
        // Add regeneration to the caster as well
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, true, true, true));
        player.sendMessage(ChatColor.GREEN + "Regen aura applied to you and trusted allies!");
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        
        player.sendMessage(ChatColor.GREEN + "Healing beam active...");
        setAbility2Active(player, true);
        
        // Shoot a healing beam that heals allies 1 heart per second
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 100) { // Run for 5 seconds (100 ticks)
                    setAbility2Active(player, false);
                    cancel();
                    return;
                }
                
                // Only heal every 20 ticks (1 second) to avoid spam
                if (ticks % 20 == 0) {
                    // Ray trace to find target
                    RayTraceResult rt = player.rayTraceEntities(20);
                    if (rt != null && rt.getHitEntity() instanceof LivingEntity target) {
                        // Check if target is an ally (player or trusted)
                        if (target instanceof Player targetPlayer) {
                            if (targetPlayer.equals(player) || context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                // Heal 1 heart (2 HP)
                                double newHealth = Math.min(targetPlayer.getMaxHealth(), targetPlayer.getHealth() + 2.0);
                                targetPlayer.setHealth(newHealth);
                                
                                // Show healing particles
                                Location hitLoc = rt.getHitPosition().toLocation(player.getWorld());
                                player.getWorld().spawnParticle(Particle.HEART, hitLoc, 3, 0.2, 0.2, 0.2, 0.0);
                            }
                        }
                    }
                }
                
                // Draw healing beam using heart particles every tick for smooth effect (moves with player)
                // Start beam 2 blocks away from player to avoid vision obstruction
                Vector dir = player.getLocation().getDirection().normalize();
                Location eye = player.getEyeLocation();
                for (double d = 2.0; d <= 20; d += 1.0) {
                    Location pt = eye.clone().add(dir.clone().multiply(d));
                    player.getWorld().spawnParticle(Particle.HEART, pt, 1, 0.05, 0.05, 0.05, 0);
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L); // Run every tick for smooth particles
        
        return true;
    }

    private void growIfCrop(Block b) {
        if (b == null) return;
        var data = b.getBlockData();
        if (data instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            b.setBlockData(ageable, true);
        }
    }
}