package hs.elementPlugin.elements.impl.frost.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Handles Frost element passive effects that need continuous checking
 * - Speed 2 when wearing leather boots (Upside 1)
 * - Speed 3 on ice (Upside 2, requires upgrade level 2)
 */
public class FrostPassiveListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public FrostPassiveListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        startPassiveEffectTask();
    }

    /**
     * Start a task that continuously checks and applies Frost passive effects
     */
    private void startPassiveEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // Check if player has Frost element
                    if (elementManager.getPlayerElement(player) != ElementType.FROST) {
                        continue;
                    }

                    var playerData = elementManager.data(player.getUniqueId());
                    int upgradeLevel = playerData.getUpgradeLevel(ElementType.FROST);

                    // Check for leather boots (Upside 1)
                    boolean hasLeatherBoots = isWearingLeatherBoots(player);

                    // Check if on ice (Upside 2, requires upgrade level 2)
                    boolean onIce = upgradeLevel >= 2 && isOnIce(player);

                    // Remove any existing speed effects first to avoid conflicts
                    player.removePotionEffect(PotionEffectType.SPEED);

                    // Apply appropriate speed effect
                    if (onIce) {
                        // Speed 3 on ice (takes priority) - 3 seconds duration, reapplied every second
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2, true, false, false));
                    } else if (hasLeatherBoots) {
                        // Speed 2 with leather boots - 3 seconds duration, reapplied every second
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1, true, false, false));
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every second (20 ticks)
    }

    /**
     * Check if player is wearing leather boots
     */
    private boolean isWearingLeatherBoots(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        return boots != null && boots.getType() == Material.LEATHER_BOOTS;
    }

    /**
     * Check if player is standing on ice
     */
    private boolean isOnIce(Player player) {
        Material blockBelow = player.getLocation().subtract(0, 1, 0).getBlock().getType();
        return blockBelow == Material.ICE ||
                blockBelow == Material.PACKED_ICE ||
                blockBelow == Material.BLUE_ICE ||
                blockBelow == Material.FROSTED_ICE;
    }
}