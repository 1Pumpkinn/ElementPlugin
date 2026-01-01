package saturn.elementPlugin.elements.impl.earth.listeners;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Earth Upside 1: Golden apples give 1 more heart of absorption (always active)
 * This adds an extra 2.0 absorption (1 heart) on top of vanilla golden apple effects
 */
public class EarthGoldenAppleListener implements Listener {
    private final ElementManager elementManager;

    public EarthGoldenAppleListener(ElementManager elementManager) {
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGoldenAppleEat(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Material item = event.getItem().getType();

        // Check if it's a golden apple or enchanted golden apple
        if (item != Material.GOLDEN_APPLE && item != Material.ENCHANTED_GOLDEN_APPLE) {
            return;
        }

        // Check if player has Earth element
        var pd = elementManager.data(player.getUniqueId());
        if (pd.getCurrentElement() != ElementType.EARTH) {
            return;
        }

        // Schedule to apply after vanilla effects (1 tick delay)
        org.bukkit.Bukkit.getScheduler().runTaskLater(
                elementManager.getPlugin(),
                () -> {
                    if (!player.isOnline()) return;

                    // Get current absorption effect
                    PotionEffect currentAbsorption = player.getPotionEffect(PotionEffectType.ABSORPTION);

                    if (currentAbsorption != null) {
                        // Add 1 more amplifier level (2.0 absorption = 1 heart)
                        int newAmplifier = currentAbsorption.getAmplifier() + 1;

                        player.addPotionEffect(new PotionEffect(
                                PotionEffectType.ABSORPTION,
                                currentAbsorption.getDuration(),
                                newAmplifier,
                                currentAbsorption.isAmbient(),
                                currentAbsorption.hasParticles(),
                                currentAbsorption.hasIcon()
                        ));

                        // Visual feedback
                        player.sendMessage(org.bukkit.ChatColor.GOLD + "✦ Earth's Bounty: +1 extra absorption heart!");
                        player.getWorld().spawnParticle(
                                org.bukkit.Particle.HAPPY_VILLAGER,
                                player.getLocation().add(0, 1, 0),
                                10, 0.3, 0.3, 0.3, 0.05
                        );
                    }
                },
                1L
        );
    }
}