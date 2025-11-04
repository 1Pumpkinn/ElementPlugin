package hs.elementPlugin.elements.abilities.impl.metal;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class MetalChainAbility extends BaseAbility {
    private final ElementPlugin plugin;

    public MetalChainAbility(ElementPlugin plugin) {
        super("metal_chain", 50, 10, 1);
        this.plugin = plugin;
    }

    @Override
    public boolean execute(ElementContext context) {
        Player player = context.getPlayer();

        // Raycast to find target
        RayTraceResult result = player.rayTraceEntities(20);

        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage(ChatColor.RED + "No target found!");
            return false;
        }

        // Don't target self or trusted players
        if (target.equals(player)) {
            return false;
        }

        if (target instanceof Player targetPlayer) {
            if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot chain trusted players!");
                return false;
            }
        }

        // Play sound
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 1.0f);

        // Create chain particle effect
        Location playerLoc = player.getEyeLocation();
        Location targetLoc = target.getEyeLocation();
        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
        double distance = playerLoc.distance(targetLoc);

        for (double d = 0; d <= distance; d += 0.5) {
            Location particleLoc = playerLoc.clone().add(direction.clone().multiply(d));
            player.getWorld().spawnParticle(Particle.FIREWORK, particleLoc, 1, 0.05, 0.05, 0.05, 0, null, true);
        }

        // Pull target towards player
        Vector pullVector = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
        pullVector.setY(pullVector.getY() + 0.5); // Add upward component
        pullVector.multiply(2.5); // Pull strength

        target.setVelocity(pullVector);

        player.sendMessage(ChatColor.GRAY + "Chained enemy!");

        return true;
    }

    @Override
    public String getName() {
        return ChatColor.GRAY + "Chain Reel";
    }

    @Override
    public String getDescription() {
        return "Pull a targeted enemy towards you with a chain. (50 mana)";
    }
}