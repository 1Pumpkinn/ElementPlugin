package hs.elementPlugin.elements.abilities.impl.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class DeathSummonUndeadAbility extends BaseAbility {
    private final ElementPlugin plugin;
    private static final List<Class<? extends LivingEntity>> UNDEAD_TYPES = List.of(
            Zombie.class, Skeleton.class, Husk.class, Stray.class // Bogged is 1.20+ only, add if available
    );
    public static final String META_FRIENDLY_UNDEAD_OWNER = "death_friendly_undead_owner";
    private final Random random = new Random();

    public DeathSummonUndeadAbility(ElementPlugin plugin) {
        super("death_summon_undead", 75, 30, 2);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        spawnLoc.setY(player.getLocation().getY());

        // Pick a random undead type
        Class<? extends LivingEntity> mobClass = UNDEAD_TYPES.get(random.nextInt(UNDEAD_TYPES.size()));
        LivingEntity mob = (LivingEntity) player.getWorld().spawn(spawnLoc, mobClass);
        mob.setCustomName(ChatColor.DARK_PURPLE + player.getName() + "'s Undead");
        mob.setCustomNameVisible(true);
        mob.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
        mob.setHealth(40.0);
        mob.setMetadata(META_FRIENDLY_UNDEAD_OWNER, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        mob.setRemoveWhenFarAway(false);
        player.getWorld().playSound(spawnLoc, Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.7f);
        player.sendMessage(ChatColor.DARK_PURPLE + "Undead ally summoned!");

        // Remove after 30 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (mob.isValid()) {
                    mob.remove();
                    player.sendMessage(ChatColor.DARK_PURPLE + "Your undead ally has returned to the grave.");
                }
            }
        }.runTaskLater(plugin, 30 * 20L);
        return true;
    }

    @Override
    public String getName() {
        return ChatColor.DARK_PURPLE + "Summon Undead";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Summon a random undead ally with 20 hearts for 30 seconds. (75 mana)";
    }

    public static String getMetaFriendlyUndeadOwner() {
        return META_FRIENDLY_UNDEAD_OWNER;
    }
}
