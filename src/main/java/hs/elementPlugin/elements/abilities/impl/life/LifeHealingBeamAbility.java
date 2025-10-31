package hs.elementPlugin.elements.abilities.impl.life;

import hs.elementPlugin.elements.abilities.BaseAbility;
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
        super("life_healing_beam", 75, 15, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        
        setActive(player, true);
        
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 100) {
                    setActive(player, false);
                    cancel();
                    return;
                }
                
                if (ticks % 20 == 0) {
                    RayTraceResult rt = player.rayTraceEntities(20);
                    if (rt != null && rt.getHitEntity() instanceof LivingEntity target) {
                        if (target instanceof Player targetPlayer) {
                            if (targetPlayer.equals(player) || context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                                double newHealth = Math.min(targetPlayer.getMaxHealth(), targetPlayer.getHealth() + 2.0);
                                targetPlayer.setHealth(newHealth);
                                
							Location hitLoc = rt.getHitPosition().toLocation(player.getWorld());
							player.getWorld().spawnParticle(Particle.HEART, hitLoc, 3, 0.2, 0.2, 0.2, 0.0, null, true);
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
					player.getWorld().spawnParticle(Particle.HEART, pt, 1, 0.05, 0.05, 0.05, 0, null, true);
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