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
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Monitors and maintains element passive effects continuously
 */
public class PassiveEffectMonitor implements Listener {

    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public PassiveEffectMonitor(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        startMonitoring();
    }

    private void startMonitoring() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    checkAndRestoreEffects(player);
                }
            }
        }.runTaskTimer(plugin, 40L, 40L);
    }

    private void checkAndRestoreEffects(Player player) {
        PlayerData pd = elementManager.data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement == null) return;

        int upgradeLevel = pd.getUpgradeLevel(currentElement);

        switch (currentElement) {
            case WATER -> checkWaterEffects(player, upgradeLevel);
            case FIRE -> checkFireEffects(player);
            case EARTH -> checkEarthEffects(player);
            case LIFE -> checkLifeEffects(player);
            case DEATH -> checkDeathEffects(player);
            case METAL -> checkMetalEffects(player);
        }
    }

    private void checkWaterEffects(Player player, int upgradeLevel) {
        if (!hasEffect(player, PotionEffectType.WATER_BREATHING)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, Integer.MAX_VALUE, 0, true, false));
        }

        if (!hasEffect(player, PotionEffectType.CONDUIT_POWER)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));
        }

        if (upgradeLevel >= 2 && !hasEffect(player, PotionEffectType.DOLPHINS_GRACE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, Integer.MAX_VALUE, 4, true, false));
        }
    }

    private void checkFireEffects(Player player) {
        if (!hasEffect(player, PotionEffectType.FIRE_RESISTANCE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
        }
    }

    private void checkEarthEffects(Player player) {
        if (!hasEffect(player, PotionEffectType.HERO_OF_THE_VILLAGE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
        }
    }

    private void checkLifeEffects(Player player) {
        if (!hasEffect(player, PotionEffectType.REGENERATION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false));
        }

        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null && attr.getBaseValue() != 30.0) {
            attr.setBaseValue(30.0);
            if (!player.isDead() && player.getHealth() > 30.0) {
                player.setHealth(30.0);
            }
        }
    }

    private void checkDeathEffects(Player player) {
        if (!hasEffect(player, PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false));
        }
    }

    private void checkMetalEffects(Player player) {
        if (!hasEffect(player, PotionEffectType.HASTE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));
        }
    }

    private boolean hasEffect(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        return effect != null && effect.getDuration() > 100;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMilkConsume(PlayerItemConsumeEvent event) {
        if (event.getItem().getType() != org.bukkit.Material.MILK_BUCKET) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                elementManager.applyUpsides(player);
                player.sendMessage(org.bukkit.ChatColor.YELLOW + "Your element effects have been restored!");
            }
        }, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEffectRemove(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getCause() != EntityPotionEffectEvent.Cause.COMMAND &&
                event.getCause() != EntityPotionEffectEvent.Cause.PLUGIN) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                checkAndRestoreEffects(player);
            }
        }, 2L);
    }
}