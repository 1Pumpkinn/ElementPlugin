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

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles Frost element passive speed effects
 */
public class FrostPassiveListener implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;
    private final Set<UUID> frostSpeedPlayers = new HashSet<>();

    public FrostPassiveListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        startPassiveEffectTask();
    }

    private void startPassiveEffectTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (elementManager.getPlayerElement(player) != ElementType.FROST) {
                        frostSpeedPlayers.remove(player.getUniqueId());
                        continue;
                    }

                    var playerData = elementManager.data(player.getUniqueId());
                    int upgradeLevel = playerData.getUpgradeLevel(ElementType.FROST);

                    boolean hasLeatherBoots = isWearingLeatherBoots(player);
                    boolean onIce = upgradeLevel >= 2 && isOnIce(player);

                    int desiredLevel = onIce ? 2 : (hasLeatherBoots ? 1 : -1);
                    PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);

                    if (desiredLevel == -1) {
                        if (frostSpeedPlayers.contains(player.getUniqueId())) {
                            player.removePotionEffect(PotionEffectType.SPEED);
                            frostSpeedPlayers.remove(player.getUniqueId());
                        }
                        continue;
                    }

                    boolean hasFrostSpeed = frostSpeedPlayers.contains(player.getUniqueId());
                    boolean needsRefresh = false;

                    if (!hasFrostSpeed) {
                        if (current == null) {
                            needsRefresh = true;
                        } else {
                            continue;
                        }
                    } else {
                        if (current == null) {
                            needsRefresh = true;
                        } else if (current.getAmplifier() != desiredLevel) {
                            needsRefresh = true;
                        } else if (current.getDuration() < 30) {
                            needsRefresh = true;
                        }
                    }

                    if (needsRefresh) {
                        if (current != null) {
                            player.removePotionEffect(PotionEffectType.SPEED);
                        }

                        player.addPotionEffect(
                                new PotionEffect(PotionEffectType.SPEED, 40, desiredLevel, true, false, false)
                        );

                        frostSpeedPlayers.add(player.getUniqueId());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private boolean isWearingLeatherBoots(Player player) {
        ItemStack boots = player.getInventory().getBoots();
        return boots != null && boots.getType() == Material.LEATHER_BOOTS;
    }

    //Checks if Frost User is on Ice
    private boolean isOnIce(Player player) {
        Material blockBelow = player.getLocation().add(0, -1, 0).getBlock().getType();
        return switch (blockBelow) {
            case ICE, PACKED_ICE, BLUE_ICE, FROSTED_ICE -> true;
            default -> false;

        };
    }
}