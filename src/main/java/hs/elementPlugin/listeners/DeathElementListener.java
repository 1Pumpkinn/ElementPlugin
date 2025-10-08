package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.death.DeathSummonUndeadAbility;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;

public class DeathElementListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;
    private final TrustManager trustManager;

    public DeathElementListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
        this.trustManager = plugin.getTrustManager();
    }

    // Upside 1: Raw/undead foods act as golden apples
    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        PlayerData pd = elementManager.data(player.getUniqueId());
        if (pd == null || pd.getCurrentElement() != ElementType.DEATH) return;
        Material food = event.getItem().getType();
        if (isRawOrUndeadFood(food)) {
            // Apply golden apple effects
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1)); // 10s regen II
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 0)); // 2 min absorption I
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 60 * 20, 0)); // 1 min fire resist
        }
    }

    private boolean isRawOrUndeadFood(Material food) {
        // Add more as needed
        return food == Material.ROTTEN_FLESH || food == Material.CHICKEN || food == Material.BEEF || 
               food == Material.PORKCHOP || food == Material.MUTTON || food == Material.RABBIT || 
               food == Material.COD || food == Material.SALMON;
    }

    // Upside 2: Passive hunger effect (should be called periodically)
    public void applyPassiveHunger(Player player) {
        PlayerData pd = elementManager.data(player.getUniqueId());
        if (pd == null || pd.getCurrentElement() != ElementType.DEATH || pd.getUpgradeLevel(ElementType.DEATH) < 2) return;
        for (Player target : player.getWorld().getPlayers()) {
            if (!target.equals(player) && target.getLocation().distance(player.getLocation()) <= 5) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, true, true, true)); // 3s
            }
        }
    }

    // Handle Death element summoned undead mob targeting
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (e.getEntity() instanceof Mob mob && e.getTarget() instanceof Player target) {
            if (mob.hasMetadata(DeathSummonUndeadAbility.META_FRIENDLY_UNDEAD_OWNER)) {
                try {
                    String ownerStr = mob.getMetadata(DeathSummonUndeadAbility.META_FRIENDLY_UNDEAD_OWNER).get(0).asString();
                    UUID ownerId = UUID.fromString(ownerStr);
                    
                    // Don't target owner
                    if (target.getUniqueId().equals(ownerId)) {
                        e.setCancelled(true);
                        return;
                    }
                    
                    // Don't target trusted players
                    if (trustManager.isTrusted(ownerId, target.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // Prevent Death element summoned undead mobs from damaging their owner or trusted players
    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Mob mob && e.getEntity() instanceof Player target) {
            if (mob.hasMetadata(DeathSummonUndeadAbility.META_FRIENDLY_UNDEAD_OWNER)) {
                try {
                    String ownerStr = mob.getMetadata(DeathSummonUndeadAbility.META_FRIENDLY_UNDEAD_OWNER).get(0).asString();
                    UUID ownerId = UUID.fromString(ownerStr);
                    
                    // Don't damage owner
                    if (target.getUniqueId().equals(ownerId)) {
                        e.setCancelled(true);
                        return;
                    }
                    
                    // Don't damage trusted players
                    if (trustManager.isTrusted(ownerId, target.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }
                } catch (Exception ignored) {}
            }
        }
    }
}