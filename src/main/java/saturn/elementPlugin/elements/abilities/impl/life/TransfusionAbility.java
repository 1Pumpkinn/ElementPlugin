package saturn.elementPlugin.elements.abilities.impl.life;

import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.abilities.BaseAbility;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

public class TransfusionAbility extends BaseAbility {

    private final saturn.elementPlugin.ElementPlugin plugin;

    public TransfusionAbility(saturn.elementPlugin.ElementPlugin plugin) {
        super("transfusion", 75, 15, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Ray trace to find target
        RayTraceResult rt = player.rayTraceEntities(20);
        if (rt == null || !(rt.getHitEntity() instanceof LivingEntity)) {
            player.sendMessage(ChatColor.RED + "No target found!");
            return false;
        }

        LivingEntity target = (LivingEntity) rt.getHitEntity();

        if (!(target instanceof Player targetPlayer)) {
            player.sendMessage(ChatColor.RED + "You can only swap health with players!");
            return false;
        }

        // Check if targeting self
        if (targetPlayer.equals(player)) {
            player.sendMessage(ChatColor.RED + "You cannot swap health with yourself!");
            return false;
        }


        // Store current health values
        double playerHealth = player.getHealth();
        double targetHealth = targetPlayer.getHealth();

        // Swap the health values
        player.setHealth(Math.min(targetHealth, player.getMaxHealth()));
        targetPlayer.setHealth(Math.min(playerHealth, targetPlayer.getMaxHealth()));

        // Visual and audio feedback
        Location playerLoc = player.getLocation().add(0, 1, 0);
        Location targetLoc = targetPlayer.getLocation().add(0, 1, 0);

        // Spawn particles at both locations
        player.getWorld().spawnParticle(Particle.HEART, playerLoc, 15, 0.5, 0.5, 0.5, 0.0, null, true);
        player.getWorld().spawnParticle(Particle.HEART, targetLoc, 15, 0.5, 0.5, 0.5, 0.0, null, true);

        // Draw connecting beam between players
        Location hitLoc = rt.getHitPosition().toLocation(player.getWorld());
        player.getWorld().spawnParticle(Particle.DUST, hitLoc, 20, 0.3, 0.3, 0.3, 0,
                new Particle.DustOptions(org.bukkit.Color.RED, 1.5f), true);

        // Sound effects
        player.getWorld().playSound(playerLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);
        player.getWorld().playSound(targetLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.2f);

        // Feedback messages
        player.sendMessage(ChatColor.GREEN + "Health swapped with " + targetPlayer.getName() + "!");
        targetPlayer.sendMessage(ChatColor.GREEN + "Health swapped with " + player.getName() + "!");

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.RED + "Transfusion";
    }

    @Override
    public String getDescription() {
        return "Swap all your health with a mutually trusted teammate's health.";
    }
}