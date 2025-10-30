package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

/**
 * Fire element's fireball ability - launches a fireball that damages entities
 */
public class FireballAbility extends BaseAbility implements Listener {
    private final ElementPlugin plugin;

    public FireballAbility(ElementPlugin plugin) {
        super("fire_fireball", 50, 10, 1);
        this.plugin = plugin;
        // Register this class as a listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Vector direction = player.getLocation().getDirection().normalize();

        // Launch fireball
        Fireball fireball = player.launchProjectile(Fireball.class, direction.multiply(2.0));

        // Set fireball properties
        fireball.setShooter(player);
        fireball.setYield(1.5f); // Higher explosion power for more damage
        fireball.setIsIncendiary(false); // Don't set blocks on fire

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        return true;
    }

    /**
     * Prevent fireball explosions from destroying blocks
     */
    @EventHandler
    public void onFireballExplode(EntityExplodeEvent event) {
        // Check if the explosion is from a Fireball
        if (event.getEntity() instanceof Fireball) {
            // Clear the block list to prevent terrain damage
            event.blockList().clear();
            // The explosion will still damage entities, just not blocks
        }
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Fireball";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Launch a fireball that damages enemies (50 mana)";
    }
}