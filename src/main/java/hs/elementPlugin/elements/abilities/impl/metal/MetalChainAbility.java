package hs.elementPlugin.elements.abilities.impl.metal;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.BaseAbility;
import hs.elementPlugin.elements.ElementContext;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
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

        // --- Improved target detection (cone-based) ---
        LivingEntity target = null;
        double range = 20;
        double coneAngle = Math.toRadians(25); // 25° cone
        Location eyeLoc = player.getEyeLocation();
        Vector lookDir = eyeLoc.getDirection();

        for (LivingEntity entity : player.getWorld().getLivingEntities()) {
            if (entity.equals(player)) continue;
            if (eyeLoc.distanceSquared(entity.getLocation()) > range * range) continue;

            Vector toEntity = entity.getLocation().toVector().subtract(eyeLoc.toVector());
            double angle = lookDir.angle(toEntity);

            if (angle < coneAngle) {
                target = entity;
                break;
            }
        }

        if (target == null) {
            player.sendMessage(ChatColor.RED + "No target found!");
            return false;
        }

        // Don't target trusted players
        if (target instanceof Player targetPlayer) {
            if (context.getTrustManager().isTrusted(player.getUniqueId(), targetPlayer.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You cannot chain trusted players!");
                return false;
            }
        }

        // Play sounds
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_CHAIN_PLACE, 1.0f, 0.8f);
        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_CHAIN_HIT, 1.0f, 1.0f);

        // --- Chain visuals ---
        Location playerLoc = player.getEyeLocation().subtract(0, 0.3, 0);
        Location targetLoc = target.getEyeLocation();

        // Calculate direction - but keep it STRICTLY HORIZONTAL (ignore Y)
        Vector direction = targetLoc.toVector().subtract(playerLoc.toVector());
        direction.setY(0); // Force horizontal only
        double distance = direction.length();

        if (distance < 0.1) {
            player.sendMessage(ChatColor.RED + "Target too close!");
            return false;
        }

        direction.normalize();

        // Calculate yaw to point chains horizontally at target
        // Chain blocks are vertical by default (Y-axis), so we need to:
        // 1. Rotate 90° around X-axis to make them horizontal
        // 2. Rotate around Y-axis to point at target
        double yaw = Math.atan2(-direction.getX(), direction.getZ());

        // Build rotation: First make horizontal (90° around X), then rotate to face target (Y-axis)
        Quaternionf rotation = new Quaternionf()
                .rotateY((float) yaw)           // Point at target horizontally
                .rotateX((float) (Math.PI / 2.0)); // Lay flat (vertical chain -> horizontal)

        List<BlockDisplay> chainDisplays = new ArrayList<>();
        BlockData chainBlock = Material.CHAIN.createBlockData();

        // Spawn chains along the HORIZONTAL line
        double chainLength = 0.6;
        int numChains = (int) Math.ceil(distance / chainLength);
        double actualSpacing = distance / numChains;

        // Average Y position for all chains (keep them at same height)
        double chainY = (playerLoc.getY() + targetLoc.getY()) / 2.0;

        for (int i = 0; i <= numChains; i++) {
            double d = i * actualSpacing;
            Location chainLoc = playerLoc.clone().add(direction.clone().multiply(d));
            chainLoc.setY(chainY); // Force all chains to same horizontal plane

            BlockDisplay display = chainLoc.getWorld().spawn(chainLoc, BlockDisplay.class, bd -> {
                bd.setBlock(chainBlock);

                Transformation transformation = bd.getTransformation();
                transformation.getLeftRotation().set(rotation);

                // Scale: make them elongated to connect better
                transformation.getScale().set(0.5f, 0.9f, 0.5f);

                bd.setTransformation(transformation);
                bd.setInterpolationDelay(0);
                bd.setInterpolationDuration(2);
            });
            chainDisplays.add(display);
        }

        // --- Pull target towards player ---
        Vector pullVector = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize();
        pullVector.setY(pullVector.getY() + 0.5); // small upward lift
        pullVector.multiply(2.5);
        target.setVelocity(pullVector);

        // --- Animate chain reeling in ---
        int totalSteps = chainDisplays.size();
        for (int i = 0; i < totalSteps; i++) {
            final int step = i;
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (step < chainDisplays.size()) {
                    BlockDisplay display = chainDisplays.get(totalSteps - 1 - step);
                    Transformation trans = display.getTransformation();
                    trans.getScale().set(0.05f, 0.05f, 0.05f);
                    display.setTransformation(trans);
                    display.setInterpolationDuration(3);

                    plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, 4L);
                }
            }, (long) (i * 1.5));
        }

        // Cleanup
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (BlockDisplay display : chainDisplays) {
                if (display != null && !display.isDead()) {
                    display.remove();
                }
            }
        }, (long) (totalSteps * 1.5 + 20));

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