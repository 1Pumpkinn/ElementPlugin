package hs.event.LifeDeathEvent;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listens for hostile mob kills during the Life/Death event
 */
public class HostileMobListener implements Listener {
    private final PointSystem pointSystem;
    private final MessageSystem messageSystem;

    // Define hostile mobs
    private static final Set<EntityType> HOSTILE_MOBS = EnumSet.of(
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.SPIDER,
            EntityType.CREEPER,
            EntityType.ENDERMAN,
            EntityType.WITCH,
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.MAGMA_CUBE,
            EntityType.SLIME,
            EntityType.SILVERFISH,
            EntityType.CAVE_SPIDER,
            EntityType.ENDERMITE,
            EntityType.GUARDIAN,
            EntityType.ELDER_GUARDIAN,
            EntityType.SHULKER,
            EntityType.HUSK,
            EntityType.STRAY,
            EntityType.WITHER_SKELETON,
            EntityType.VEX,
            EntityType.VINDICATOR,
            EntityType.EVOKER,
            EntityType.PHANTOM,
            EntityType.DROWNED,
            EntityType.PILLAGER,
            EntityType.RAVAGER,
            EntityType.HOGLIN,
            EntityType.ZOGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.WARDEN,
            EntityType.BOGGED,
            EntityType.BREEZE
    );

    public HostileMobListener(PointSystem pointSystem, MessageSystem messageSystem) {
        this.pointSystem = pointSystem;
        this.messageSystem = messageSystem;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHostileMobKill(EntityDeathEvent event) {
        // Only track if event is active
        if (!pointSystem.isEventActive()) return;

        // Check if killed entity is a hostile mob
        if (!HOSTILE_MOBS.contains(event.getEntityType())) return;

        // Check if killer is a player
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Add hostile kill point
        pointSystem.addHostileKill(killer.getUniqueId());

        // Send message to player
        int currentKills = pointSystem.getHostileKills(killer.getUniqueId());
        messageSystem.sendHostileKillMessage(killer, currentKills);
    }
}