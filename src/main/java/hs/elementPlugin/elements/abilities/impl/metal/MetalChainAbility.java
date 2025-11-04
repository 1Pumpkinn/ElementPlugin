package hs.elementPlugin.elements.abilities.impl.metal;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

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

        // Create chain visual using BlockDisplay entities
        Location playerLoc = player.getEyeLocation().subtract(0, 0.3, 0); // Start slightly lower
        Location targetLoc = target.getEyeLocation();
        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector());
        double distance = direction.length();
        direction.setY(0); // Ignore Y-axis to make sure chains are horizontal
        direction.normalize(); // Normalize the direction to make sure it's a unit vector

        // Store display entities for cleanup
        List<BlockDisplay> chainDisplays = new ArrayList<>();

        // Chain block data
        BlockData chainBlock = Material.CHAIN.createBlockData();

        // Spacing that ensures chains connect - use smaller spacing with uniform scale
        double chainLength = 0.5; // How much space each chain segment takes up
        int numChains = (int) Math.ceil(distance / chainLength);
        double actualSpacing = distance / numChains; // Evenly distribute chains

        // Create each chain segment
        for (int i = 0; i <= numChains; i++) {
            double d = i * actualSpacing;
            Location chainLoc = playerLoc.clone().add(direction.clone().multiply(d));

            BlockDisplay display = chainLoc.getWorld().spawn(chainLoc, BlockDisplay.class, bd -> {
                bd.setBlock(chainBlock);

                // Calculate rotation so chains face horizontally between player and target
                // Horizontal direction along X and Z (ignore Y-axis)
                Vector3f targetDir = new Vector3f((float) direction.getX(), 0, (float) direction.getZ()); // Horizontal vector
                targetDir.normalize(); // Make sure it's a unit vector

                // Rotation to face the player and target horizontally
                Vector3f yAxis = new Vector3f(0, 1, 0); // Y-axis rotation for horizontal alignment
                Quaternionf rotation = new Quaternionf();
                rotation.rotationTo(yAxis, targetDir);

                // Apply transformation with this horizontal rotation
                Transformation transformation = bd.getTransformation();
                transformation.getLeftRotation().set(rotation);

                // Apply uniform scale
                transformation.getScale().set(0.55f, 0.55f, 0.55f); // Uniform scale for all chains

                bd.setTransformation(transformation);
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            });

            chainDisplays.add(display);
        }

        // Pull target towards player
        Vector pullVector = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
        pullVector.setY(pullVector.getY() + 0.5); // Add upward component
        pullVector.multiply(2.5); // Pull strength
        target.setVelocity(pullVector);

        // Animate the chain reeling in
        int totalSteps = chainDisplays.size();
        for (int i = 0; i < totalSteps; i++) {
            final int step = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (step < chainDisplays.size()) {
                    BlockDisplay display = chainDisplays.get(totalSteps - 1 - step);

                    // Shrink and remove the block display
                    Transformation trans = display.getTransformation();
                    trans.getScale().set(0.05f, 0.05f, 0.05f);
                    display.setTransformation(trans);
                    display.setInterpolationDuration(3);

                    plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, 4L);
                }
            }, (long) (i * 1.5)); // Smooth reel-in timing
        }

        // Final cleanup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (BlockDisplay display : chainDisplays) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
            }
        }, (long) (totalSteps * 1.5 + 20));

        // Send message
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
