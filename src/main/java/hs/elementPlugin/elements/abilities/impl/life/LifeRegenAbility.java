package hs.elementPlugin.elements.abilities.impl.life;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class LifeRegenAbility extends BaseAbility {

    private final hs.elementPlugin.ElementPlugin plugin;
    
    public LifeRegenAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("life_regen", 50, 20, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
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
                    
					player.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0.1, 0.1, 0.1, 0, new Particle.DustOptions(org.bukkit.Color.RED, 1.0f), true);
                }
                tick++;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
        
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

    public String getName() {
        return ChatColor.GREEN + "Regeneration Aura";
    }

    public String getDescription() {
        return "Grant regeneration to yourself and trusted allies within 5 blocks.";
    }
}