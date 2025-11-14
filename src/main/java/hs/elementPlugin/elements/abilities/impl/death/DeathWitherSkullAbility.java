package hs.elementPlugin.elements.abilities.impl.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

public class DeathWitherSkullAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public DeathWitherSkullAbility(ElementPlugin plugin) {
        super("death_wither_skull", 75, 10, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Vector direction = player.getLocation().getDirection().normalize();
        WitherSkull skull = player.launchProjectile(WitherSkull.class, direction.multiply(2.0));
        skull.setShooter(player);
        skull.setYield(1.5f); // Explosion power
        skull.setIsIncendiary(false);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 1f);
        return true;
    }

    @Override
    public String getName() {
        return ChatColor.DARK_PURPLE + "Wither Skull";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Shoot a wither skull that explodes on impact. (50 mana)";
    }
}
