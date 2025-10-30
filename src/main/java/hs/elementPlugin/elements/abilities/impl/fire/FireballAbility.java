package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Fire element's fireball ability - launches a fireball that damages entities
 */
public class FireballAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public FireballAbility(ElementPlugin plugin) {
        super("fire_fireball", 50, 10, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Launch fireball in the direction player is looking
        Vector direction = player.getLocation().getDirection().normalize();
        Fireball fireball = player.launchProjectile(Fireball.class, direction.multiply(2.0));

        // Set fireball properties
        fireball.setShooter(player);
        fireball.setYield(0.0f); // No explosion power - won't destroy terrain
        fireball.setIsIncendiary(false); // Don't set blocks on fire

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1.0f, 1.0f);

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Fireball";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Launch a fireball that damages enemies on impact. (50 mana)";
    }
}