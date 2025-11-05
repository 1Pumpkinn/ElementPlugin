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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Random;

/**
 * Fire element's meteor shower ability - rains down fireballs from above
 */
public class MeteorShowerAbility extends BaseAbility implements Listener {
    private final ElementPlugin plugin;
    private final Random random = new Random();

    public MeteorShowerAbility(ElementPlugin plugin) {
        super("fire_meteor_shower", 75, 30, 2);
        this.plugin = plugin;
        // Register this class as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Use player's current location instead of where they're looking
        Location targetLoc = player.getLocation();

        player.getWorld().playSound(targetLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.5f);

        // Spawn meteors over 6 seconds - MUCH more meteors, faster spawn rate
        new BukkitRunnable() {
            int count = 0;
            final int maxMeteors = 18;

            @Override
            public void run() {
                if (count >= maxMeteors || !player.isOnline()) {
                    cancel();
                    return;
                }

                // Random location around player's position (MUCH tighter spread)
                double offsetX = (random.nextDouble() - 0.5) * 6; // Reduced from 12 to 6
                double offsetZ = (random.nextDouble() - 0.5) * 6; // Reduced from 12 to 6
                Location spawnLoc = targetLoc.clone().add(offsetX, 25, offsetZ);

                // Spawn fireball falling downward with slight angle variation for realism
                Fireball fireball = player.getWorld().spawn(spawnLoc, Fireball.class);
                fireball.setShooter(player);

                // Add slight random horizontal velocity for more natural meteor fall
                double randomX = (random.nextDouble() - 0.5) * 0.3;
                double randomZ = (random.nextDouble() - 0.5) * 0.3;
                fireball.setDirection(new Vector(randomX, -1, randomZ));

                fireball.setYield(1.5f); // Higher yield for damage
                fireball.setIsIncendiary(false); // Don't set blocks on fire


                // Spawn more dramatic particles at spawn location
                player.getWorld().spawnParticle(Particle.FLAME, spawnLoc, 30, 0.8, 0.8, 0.8, 0.15, null, true);
                player.getWorld().spawnParticle(Particle.LAVA, spawnLoc, 5, 0.5, 0.5, 0.5, 0.0, null, true);
                player.getWorld().playSound(spawnLoc, Sound.ENTITY_BLAZE_SHOOT, 0.6f, 0.7f);

                count++;
            }
        }.runTaskTimer(plugin, 0L, 5L); // Every 0.25 seconds (5 ticks) - much faster spawn rate

        return true;
    }

    /**
     * Prevent meteor explosions from destroying blocks
     */
    @EventHandler
    public void onMeteorExplode(EntityExplodeEvent event) {
        // Check if the explosion is from a Fireball
        if (event.getEntity() instanceof Fireball) {
            // Clear the block list to prevent terrain damage
            event.blockList().clear();
            // The explosion will still damage entities, just not blocks
        }
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