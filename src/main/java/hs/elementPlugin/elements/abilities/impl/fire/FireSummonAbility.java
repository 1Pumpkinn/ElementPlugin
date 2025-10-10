package hs.elementPlugin.elements.abilities.impl.fire;

import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class FireSummonAbility extends BaseAbility {
    public static final String META_FRIENDLY_BLAZE_OWNER = "fire_friendly_owner";
    private final hs.elementPlugin.ElementPlugin plugin;
    
    public FireSummonAbility(hs.elementPlugin.ElementPlugin plugin) {
        super("fire_summon", 100, 30, 1);
        this.plugin = plugin;
    }
    
    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();
        
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 1f, 0.8f);
        player.sendMessage(ChatColor.GOLD + "Summoning friendly blaze...");

        Location spawnLoc = player.getLocation().add(player.getLocation().getDirection().multiply(3));
        spawnLoc.setY(player.getLocation().getY());

        // Spawn 2 blazes instead of 1
        for (int i = 0; i < 2; i++) {
            Location blazeSpawnLoc = spawnLoc.clone().add(
                (i == 0) ? -1 : 1, 0, 0
            );
            
            Blaze blaze = player.getWorld().spawn(blazeSpawnLoc, Blaze.class);
            blaze.setMetadata(META_FRIENDLY_BLAZE_OWNER, new FixedMetadataValue(context.getPlugin(), player.getUniqueId().toString()));
            
            // Make the blaze stronger
            blaze.getAttribute(Attribute.MAX_HEALTH).setBaseValue(40.0);
            blaze.setHealth(40.0);
            
            // Set custom name
            blaze.setCustomName(ChatColor.GOLD + player.getName() + "'s Blaze " + (i + 1));
            blaze.setCustomNameVisible(true);
            
            // Remove the blaze after 30 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (blaze.isValid()) {
                        blaze.getWorld().spawnParticle(Particle.FLAME, blaze.getLocation(), 8, 0.3, 0.3, 0.3, 0.05);
                        blaze.remove();
                        player.sendMessage(ChatColor.GOLD + "Your blaze has returned to the Nether.");
                    }
                }
            }.runTaskLater(context.getPlugin(), 30 * 20L);
        }

        return true;
    }
    
    public void clearEffects(Player player) {
        // No effects to clear
    }
    
    @Override
    public String getName() {
        return ChatColor.RED + "Summon Blaze";
    }
    
    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Summon a friendly blaze to fight for you. (100 mana)";
    }
}