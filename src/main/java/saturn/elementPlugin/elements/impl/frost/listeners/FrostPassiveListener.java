package saturn.elementPlugin.elements.impl.frost.listeners;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Frost passive speed effects
 *
 * Upside 1: Speed I (always active for Frost users)
 * Upside 2: Speed II when wearing full IRON trim armor (requires Upgrade II)
 */
public class FrostPassiveListener implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    // Track players who currently have Frost-applied Speed
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

                    // Skip non-Frost players
                    if (elementManager.getPlayerElement(player) != ElementType.FROST) {
                        // Only remove Frost-applied Speed
                        if (frostSpeedPlayers.remove(player.getUniqueId())) {
                            player.removePotionEffect(PotionEffectType.SPEED);
                        }
                        continue;
                    }

                    var pd = elementManager.data(player.getUniqueId());
                    int upgradeLevel = pd.getUpgradeLevel(ElementType.FROST);

                    // Check if Upgrade II + full iron-trim armor
                    boolean hasIronTrim = upgradeLevel >= 2 && hasFullIronTrimArmor(player);

                    // Determine speed level:
                    // Speed I (amplifier 0) by default
                    // Speed II (amplifier 1) if iron-trim armor
                    int desiredAmplifier = hasIronTrim ? 1 : 0;

                    PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);

                    // Refresh if:
                    // - Player doesn't have Frost-applied Speed yet
                    // - Amplifier changed
                    // - Duration too low (< 30 ticks)
                    boolean needsRefresh =
                            !frostSpeedPlayers.contains(player.getUniqueId()) ||
                                    current == null ||
                                    current.getAmplifier() != desiredAmplifier ||
                                    current.getDuration() < 30;

                    if (needsRefresh) {
                        // Remove only Frost-applied Speed
                        if (frostSpeedPlayers.contains(player.getUniqueId())) {
                            player.removePotionEffect(PotionEffectType.SPEED);
                        }

                        // Apply new Frost speed
                        player.addPotionEffect(
                                new PotionEffect(
                                        PotionEffectType.SPEED,
                                        40, // 2 seconds, refreshed every second
                                        desiredAmplifier,
                                        true,
                                        false,
                                        false
                                )
                        );

                        frostSpeedPlayers.add(player.getUniqueId());
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // Run every 20 ticks (1 second)
    }

    /**
     * Checks if the player is wearing full armor with IRON trim
     */
    private boolean hasFullIronTrimArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();

        for (ItemStack piece : armor) {
            if (piece == null || !(piece.getItemMeta() instanceof ArmorMeta armorMeta)) {
                return false;
            }

            if (!armorMeta.hasTrim()) {
                return false;
            }

            ArmorTrim trim = armorMeta.getTrim();
            if (trim.getMaterial() != TrimMaterial.IRON) {
                return false;
            }
        }

        return true;
    }
}
