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
 * Handles Frost element passive effects that need continuous checking:
 *  - Speed 1 with leather boots (Upside 1)
 *  - Speed 2 on ice (Upside 2, requires upgrade level 2)
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
     * Passive effect task — runs once per second
     */
    private void startPassiveEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {

                for (Player player : Bukkit.getOnlinePlayers()) {

                    // Only apply to Frost players
                    if (elementManager.getPlayerElement(player) != ElementType.FROST) {
                        continue;
                    }

                    var playerData = elementManager.data(player.getUniqueId());
                    int upgradeLevel = playerData.getUpgradeLevel(ElementType.FROST);

                    boolean hasLeatherBoots = isWearingLeatherBoots(player);
                    boolean onIce = upgradeLevel >= 2 && isOnIce(player);

                    // Determine the CORRECT effect level (-1 = none)
                    int desiredLevel = onIce ? 2 : (hasLeatherBoots ? 1 : -1);

                    PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);

                    if (desiredLevel == -1) {
                        // Should have NO speed
                        // Only remove if the effect was applied by THIS system
                        if (current != null && current.getDuration() > 1000) {
                            player.removePotionEffect(PotionEffectType.SPEED);
                        }
                        continue;
                    }

                    // Should have speed of specific level
                    boolean needsRefresh = (current == null) ||
                            (current.getAmplifier() != desiredLevel) ||
                            (current.getDuration() < 40);

                    if (needsRefresh) {
                        // Remove any conflicting speed effect first
                        if (current != null) {
                            player.removePotionEffect(PotionEffectType.SPEED);
                        }

                        // Reapply with long duration so we can detect our own effect later
                        player.addPotionEffect(
                                new PotionEffect(PotionEffectType.SPEED, 2000, desiredLevel, true, false, false)
                        );
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Runs every second
    }

    private boolean isWearingLeatherBoots(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        return boots != null && boots.getType() == Material.LEATHER_BOOTS;
    }

    private boolean isOnIce(Player player) {
        Material blockBelow = player.getLocation().add(0, -1, 0).getBlock().getType();
        return switch (blockBelow) {
            case ICE, PACKED_ICE, BLUE_ICE, FROSTED_ICE -> true;
            default -> false;
        };
    }
}
