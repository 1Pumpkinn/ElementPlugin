package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Fire element's meteor shower ability - rains down fireballs from above
 */
public class MeteorShowerAbility extends BaseAbility {
    private final ElementPlugin plugin;
    private final Random random = new Random();

    public MeteorShowerAbility(ElementPlugin plugin) {
        super("fire_meteor_shower", 100, 30, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Use player's current location instead of where they're looking
        Location targetLoc = player.getLocation();

        player.sendMessage(ChatColor.GOLD + "Meteor shower incoming!");
        player.getWorld().playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);

        // Spawn meteors over 8 seconds (longer duration, less spammy)
        new BukkitRunnable() {
            int count = 0;
            final int maxMeteors = 8;

            @Override
            public void run() {
                if (count >= maxMeteors || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Random location around player's position
                double offsetX = (random.nextDouble() - 0.5) * 10;
                double offsetZ = (random.nextDouble() - 0.5) * 10;
                Location spawnLoc = targetLoc.clone().add(offsetX, 20, offsetZ);

                // Spawn fireball falling downward
                Fireball fireball = player.getWorld().spawn(spawnLoc, Fireball.class);
                fireball.setShooter(player);
                fireball.setDirection(new Vector(0, -1, 0));
                fireball.setYield(0.0f); // No explosion power - won't destroy terrain
                fireball.setIsIncendiary(false); // Don't set blocks on fire

                // Spawn particles at spawn location
                player.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 20, 0.5, 0.5, 0.5, 0.1, null, true);
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.8f);

                count++;
            }
        }.runTaskTimer(plugin, 0L, 20L); // Every 1 second (was 12L = 0.6 seconds)

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Meteor Shower";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Rain down fireballs from the sky around your position. (100 mana)";
    }
}