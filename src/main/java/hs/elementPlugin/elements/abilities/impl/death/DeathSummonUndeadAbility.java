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
            Zombie.class, Skeleton.class, Husk.class, Stray.class
    );
    public static final String META_FRIENDLY_UNDEAD_OWNER = "death_summoned_owner";
    public static final String META_FRIENDLY_UNDEAD_UNTIL = "death_summoned_until";
    private final Random random = new Random();

    public DeathSummonUndeadAbility(ElementPlugin plugin) {
        super("death_summon_undead", 50, 30, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(2));
        spawnLoc.setY(player.getLocation().getY());

        // Pick a random undead type
        Class<? extends LivingEntity> mobClass = UNDEAD_TYPES.get(random.nextInt(UNDEAD_TYPES.size()));
        LivingEntity entity = (LivingEntity) player.getWorld().spawn(spawnLoc, mobClass);

        // Configure the mob
        entity.setCustomName(ChatColor.DARK_PURPLE + player.getName() + "'s Undead");
        entity.setCustomNameVisible(true);
        entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
        entity.setHealth(40.0);
        entity.setRemoveWhenFarAway(false);

        // CRITICAL: Set metadata with correct keys that match the listener
        long expirationTime = System.currentTimeMillis() + 30_000L; // 30 seconds
        entity.setMetadata(META_FRIENDLY_UNDEAD_OWNER, new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        entity.setMetadata(META_FRIENDLY_UNDEAD_UNTIL, new FixedMetadataValue(plugin, expirationTime));

        // Ensure AI is enabled and mob can attack
        if (entity instanceof Mob mob) {
            mob.setAware(true);
            mob.setCanPickupItems(false);

            // Find nearest enemy to attack immediately
            Player nearestEnemy = null;
            double bestDistance = Double.MAX_VALUE;
            for (Player p : player.getWorld().getPlayers()) {
                if (p.getUniqueId().equals(player.getUniqueId())) continue;
                if (context.getTrustManager().isTrusted(player.getUniqueId(), p.getUniqueId())) continue;

                double dist = p.getLocation().distanceSquared(entity.getLocation());
                if (dist < bestDistance && dist < 20*20) {
                    bestDistance = dist;
                    nearestEnemy = p;
                }
            }

            // Set initial target if enemy found
            if (nearestEnemy != null) {
                mob.setTarget(nearestEnemy);
            }
        }

        player.getWorld().playSound(spawnLoc, Sound.ENTITY_ZOMBIE_AMBIENT, 1f, 0.7f);
        player.sendMessage(ChatColor.DARK_PURPLE + "Summoned an undead ally for 30 seconds!");

        // Schedule removal after 30 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isValid() && !entity.isDead()) {
                    entity.remove();
                    if (player.isOnline()) {
                        player.sendMessage(ChatColor.DARK_PURPLE + "Your undead ally has despawned.");
                    }
                }
            }
        }.runTaskLater(plugin, 600L); // 30 seconds = 600 ticks

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

    public static String getMetaFriendlyUndeadUntil() {
        return META_FRIENDLY_UNDEAD_UNTIL;
    }
}