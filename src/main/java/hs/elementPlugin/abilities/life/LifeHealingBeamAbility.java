package hs.elementPlugin.abilities.life;

import hs.elementPlugin.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class LifeHealingBeamAbility extends BaseAbility {

    private final hs.elementPlugin.ElementPlugin plugin;
    
    public LifeHealingBeamAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("life_healing_beam", 40, 15, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        
        player.sendMessage(ChatColor.GREEN + "Healing beam active...");
        setActive(player, true);
        
        // Shoot a healing beam that heals allies 1 heart per second
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 100) { // Run for 5 seconds (100 ticks)
                    setActive(player, false);
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

    @Override
    public String getName() {
        return ChatColor.GREEN + "Healing Beam";
    }

    @Override
    public String getDescription() {
        return "Project a beam of healing energy that restores health to trusted allies.";
    }
}