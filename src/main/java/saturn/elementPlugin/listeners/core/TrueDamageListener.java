package saturn.elementPlugin.listeners.core;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import saturn.elementPlugin.elements.abilities.impl.metal.MetalDashAbility;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Handles true damage bypass for Death Slash and Metal Dash
 * True damage ignores armor but DOES trigger totems
 *
 * CRITICAL: Runs at LOWEST priority to intercept damage BEFORE armor calculations
 * but DOES NOT cancel the event, allowing totems to trigger normally
 */
public class TrueDamageListener implements Listener {

    private final ElementPlugin plugin;
    private static final long METADATA_TIMEOUT_MS = 100; // 100ms window (increased for reliability)

    public TrueDamageListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Check if this entity has the TRUE_DAMAGE metadata
        String metaKey = null;
        long timestamp = 0;

        // Check Metal Dash true damage
        if (entity.hasMetadata(MetalDashAbility.META_TRUE_DAMAGE)) {
            metaKey = MetalDashAbility.META_TRUE_DAMAGE;
            timestamp = entity.getMetadata(metaKey).get(0).asLong();
        }
        // Check Death Slash true damage
        else if (entity.hasMetadata(DeathSlashAbility.META_TRUE_DAMAGE)) {
            metaKey = DeathSlashAbility.META_TRUE_DAMAGE;
            timestamp = entity.getMetadata(metaKey).get(0).asLong();
        }

        // No true damage metadata found
        if (metaKey == null) return;

        // Validate timestamp (prevent stale metadata from affecting damage)
        long currentTime = System.currentTimeMillis();
        if (currentTime - timestamp > METADATA_TIMEOUT_MS) {
            // Metadata expired, remove it and don't apply true damage
            entity.removeMetadata(metaKey, plugin);
            plugin.getLogger().fine("Expired true damage metadata for " + entity.getName());
            return;
        }

        // === APPLY TRUE DAMAGE ===
        // TRUE DAMAGE bypasses armor and resistance modifiers
        // but DOES NOT cancel the event, so totems will still trigger

        // Zero out all damage modifiers (armor, magic, resistance, etc.)
        event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
        event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0);
        event.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE, 0);
        event.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, 0);
        event.setDamage(EntityDamageEvent.DamageModifier.HARD_HAT, 0);

        // CRITICAL: DO NOT cancel the event - let it proceed normally
        // This ensures totems will trigger if the damage would be fatal

        // Clean up metadata after processing
        entity.removeMetadata(metaKey, plugin);

        double finalDamage = event.getFinalDamage();
        plugin.getLogger().fine("Applied TRUE DAMAGE (" + finalDamage + ") to " +
                entity.getName() + " - totems will still trigger if fatal");
    }
}