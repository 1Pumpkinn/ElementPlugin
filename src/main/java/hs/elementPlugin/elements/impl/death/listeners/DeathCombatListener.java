package hs.elementPlugin.elements.impl.death.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import hs.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/**
 * Listener for Death element combat abilities (Death Clock and Slash)
 */
public class DeathCombatListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elementManager;

    public DeathCombatListener(ElementPlugin plugin, ElementManager elementManager) {
        this.plugin = plugin;
        this.elementManager = elementManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAnyDamage(EntityDamageByEntityEvent event) {
        plugin.getLogger().info("[DeathCombat] ANY DAMAGE EVENT DETECTED!");
        plugin.getLogger().info("[DeathCombat] Damager: " + event.getDamager().getType());
        plugin.getLogger().info("[DeathCombat] Entity: " + event.getEntity().getType());
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        // Check if attacker is a player
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        // Check if target is a living entity
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        // DEBUG: Log all events
        plugin.getLogger().info("[DeathCombat] Player " + attacker.getName() + " hit " + target.getName());

        // Verify attacker has Death element
        var playerData = elementManager.data(attacker.getUniqueId());
        if (playerData == null || playerData.getCurrentElement() != ElementType.DEATH) {
            plugin.getLogger().info("[DeathCombat] Player doesn't have Death element");
            return;
        }

        plugin.getLogger().info("[DeathCombat] Player has Death element!");

        // Don't apply to trusted players
        if (target instanceof Player targetPlayer) {
            if (plugin.getTrustManager().isTrusted(attacker.getUniqueId(), targetPlayer.getUniqueId())) {
                plugin.getLogger().info("[DeathCombat] Target is trusted, skipping");
                return;
            }
        }

        // Check for Death Clock (Ability 1) - must check BEFORE removing metadata
        boolean hasDeathClock = attacker.hasMetadata(DeathClockAbility.META_DEATH_CLOCK_ACTIVE);
        plugin.getLogger().info("[DeathCombat] Has Death Clock: " + hasDeathClock);

        // Check for Slash (Ability 2) - must check BEFORE removing metadata
        boolean hasSlash = attacker.hasMetadata(DeathSlashAbility.META_SLASH_ACTIVE);
        plugin.getLogger().info("[DeathCombat] Has Slash: " + hasSlash);

        // Apply Death Clock if active
        if (hasDeathClock) {
            attacker.sendMessage(org.bukkit.ChatColor.DARK_PURPLE + "[DEBUG] Applying Death Clock effects!");
            plugin.getLogger().info("[DeathCombat] Applying Death Clock effects");
            DeathClockAbility.applyEffects(plugin, attacker, target);
        }

        // Apply Slash if active
        if (hasSlash) {
            attacker.sendMessage(org.bukkit.ChatColor.RED + "[DEBUG] Applying Slash effects!");
            plugin.getLogger().info("[DeathCombat] Applying Slash effects");
            DeathSlashAbility.applyBleeding(plugin, attacker, target);
        }
    }
}