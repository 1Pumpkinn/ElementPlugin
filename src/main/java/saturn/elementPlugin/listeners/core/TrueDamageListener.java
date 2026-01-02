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
 */
public class TrueDamageListener implements Listener {

    private final ElementPlugin plugin;
    private static final long METADATA_TIMEOUT_MS = 50; // 50ms window to apply true damage

    public TrueDamageListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity entity)) return;

        // Check if this entity has the TRUE_DAMAGE metadata
        if (!entity.hasMetadata(MetalDashAbility.META_TRUE_DAMAGE) &&
                !entity.hasMetadata(DeathSlashAbility.META_TRUE_DAMAGE)) {
            return;
        }

        // Check which metadata is present and validate timestamp
        String metaKey = null;
        long timestamp = 0;

        if (entity.hasMetadata(MetalDashAbility.META_TRUE_DAMAGE)) {
            metaKey = MetalDashAbility.META_TRUE_DAMAGE;
            timestamp = entity.getMetadata(metaKey).get(0).asLong();
        } else if (entity.hasMetadata(DeathSlashAbility.META_TRUE_DAMAGE)) {
            metaKey = DeathSlashAbility.META_TRUE_DAMAGE;
            timestamp = entity.getMetadata(metaKey).get(0).asLong();
        }

        if (metaKey == null) return;

        // Check if metadata is still valid (within timeout window)
        long currentTime = System.currentTimeMillis();
        if (currentTime - timestamp > METADATA_TIMEOUT_MS) {
            // Metadata expired, remove it and don't apply true damage
            entity.removeMetadata(metaKey, plugin);
            return;
        }

        // TRUE DAMAGE: Set damage to raw damage (bypasses armor reduction)
        // Get the original damage amount
        double originalDamage = event.getDamage();

        // Set final damage to original damage (bypasses armor)
        // This ensures damage goes through armor but STILL triggers totems
        event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
        event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0);
        event.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE, 0);

        // CRITICAL: Don't cancel the event - let it proceed so totems can trigger
        // Just zero out armor/resistance modifiers

        // Remove metadata after processing
        entity.removeMetadata(metaKey, plugin);

        plugin.getLogger().fine("Applied true damage (" + originalDamage + ") to " + entity.getName() + " - totems will still trigger");
    }
}