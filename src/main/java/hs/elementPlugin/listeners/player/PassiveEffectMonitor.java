package hs.elementPlugin.listeners.player;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Monitors and maintains element passive effects continuously.
 * Ensures players always have their element's effects even after:
 * - Drinking milk
 * - Death/respawn
 * - Other effect-clearing events
 * - Server restarts
 */
public class PassiveEffectMonitor implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public PassiveEffectMonitor(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        startMonitoring();
    }

    /**
     * Start the continuous effect monitoring task
     * Runs every 2 seconds to check and restore effects
     */
    private void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkAndRestoreEffects(player);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L); // Run every 2 seconds (40 ticks)
    }

    /**
     * Check if player has their required element effects and restore if missing
     */
    private void checkAndRestoreEffects(Player player) {
        PlayerData pd = elementManager.data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement == null) {
            return;
        }

        int upgradeLevel = pd.getUpgradeLevel(currentElement);

        // Check element-specific effects based on upgrade level
        switch (currentElement) {
            case AIR:
                // Air has no passive potion effects
                break;

            case WATER:
                checkWaterEffects(player, upgradeLevel);
                break;

            case FIRE:
                checkFireEffects(player, upgradeLevel);
                break;

            case EARTH:
                checkEarthEffects(player, upgradeLevel);
                break;

            case LIFE:
                checkLifeEffects(player, upgradeLevel);
                break;

            case DEATH:
                checkDeathEffects(player, upgradeLevel);
                break;

            case METAL:
                checkMetalEffects(player, upgradeLevel);
                break;

            case FROST:
                checkFrostEffects(player, upgradeLevel);
                break;
        }
    }

    /**
     * Check and restore Water element effects
     */
    private void checkWaterEffects(Player player, int upgradeLevel) {
        // Upside 1: Infinite Water Breathing
        if (!hasEffect(player, PotionEffectType.WATER_BREATHING)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false));
        }

        // Upside 1: Infinite Conduit Power
        if (!hasEffect(player, PotionEffectType.CONDUIT_POWER)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
        }

        // Upside 2: Dolphins Grace 5 (requires upgrade level 2)
        if (upgradeLevel >= 2) {
            if (!hasEffect(player, PotionEffectType.DOLPHINS_GRACE)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 4, true, false));
            }
        }
    }

    /**
     * Check and restore Fire element effects
     */
    private void checkFireEffects(Player player, int upgradeLevel) {
        // Upside 1: Infinite Fire Resistance
        if (!hasEffect(player, PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
        }
    }

    /**
     * Check and restore Earth element effects
     */
    private void checkEarthEffects(Player player, int upgradeLevel) {
        
        // Hero of the Village effect
        if (!hasEffect(player, PotionEffectType.HERO_OF_THE_VILLAGE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
        }
    }

    /**
     * Check and restore Life element effects
     */
    private void checkLifeEffects(Player player, int upgradeLevel) {
        // Upside 1: Permanent Regeneration I
        if (!hasEffect(player, PotionEffectType.REGENERATION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false));
        }

        // Upside 1: 15 hearts (30 HP)
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null && attr.getBaseValue() != 30.0) {
            attr.setBaseValue(30.0);
            // Don't modify current health unless it exceeds max
            if (!player.isDead() && player.getHealth() > 30.0) {
                player.setHealth(30.0);
            }
        }
    }

    /**
     * Check and restore Death element effects
     */
    private void checkDeathEffects(Player player, int upgradeLevel) {
        // Upside 1: Permanent Night Vision
        if (!hasEffect(player, PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        }
    }

    /**
     * Check and restore Metal element effects
     */
    private void checkMetalEffects(Player player, int upgradeLevel) {
        // Upside 1: Permanent Haste I
        if (!hasEffect(player, PotionEffectType.HASTE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));
        }
    }

    /**
     * Check and restore Frost element effects
     * Note: Frost effects are handled dynamically by FrostPassiveListener
     * based on conditions (leather boots, ice blocks)
     */
    private void checkFrostEffects(Player player, int upgradeLevel) {
        // Frost speed effects are conditional and managed by FrostPassiveListener
        // No constant effects to check here
    }

    /**
     * Check if player has a specific potion effect
     */
    private boolean hasEffect(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        // Consider effect present if it exists and has sufficient duration
        return effect != null && effect.getDuration() > 100;
    }

    /**
     * Handle milk bucket consumption - immediately restore effects
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMilkConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != org.bukkit.Material.MILK_BUCKET) {
            return;
        }

        Player player = event.getPlayer();

        // Schedule effect restoration for next tick (after milk clears effects)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                elementManager.applyUpsides(player);
                player.sendMessage(org.bukkit.ChatColor.YELLOW + "Your element effects have been restored!");
            }
        }, 1L);
    }
}