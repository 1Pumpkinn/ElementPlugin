package hs.event.LifeDeathEvent;

import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.EnumSet;
import java.util.Set;

/**
 * Listens for passive mob kills during the Life/Death event
 */
public class PassiveMobListener implements Listener {
    private final PointSystem pointSystem;
    private final MessageSystem messageSystem;

    // Define passive mobs
    private static final Set<EntityType> PASSIVE_MOBS = EnumSet.of(
            EntityType.COW,
            EntityType.PIG,
            EntityType.SHEEP,
            EntityType.CHICKEN,
            EntityType.RABBIT,
            EntityType.HORSE,
            EntityType.DONKEY,
            EntityType.MULE,
            EntityType.LLAMA,
            EntityType.CAT,
            EntityType.PARROT,
            EntityType.MOOSHROOM,
            EntityType.OCELOT,
            EntityType.TURTLE,
            EntityType.PANDA,
            EntityType.FOX,
            EntityType.BEE,
            EntityType.STRIDER,
            EntityType.AXOLOTL,
            EntityType.GOAT,
            EntityType.FROG,
            EntityType.TADPOLE,
            EntityType.ALLAY,
            EntityType.CAMEL,
            EntityType.SNIFFER,
            EntityType.ARMADILLO
    );

    public PassiveMobListener(PointSystem pointSystem, MessageSystem messageSystem) {
        this.pointSystem = pointSystem;
        this.messageSystem = messageSystem;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPassiveMobKill(EntityDeathEvent event) {
        // Only track if event is active
        if (!pointSystem.isEventActive()) return;

        // Check if killed entity is a passive mob
        if (!PASSIVE_MOBS.contains(event.getEntityType())) return;

        // Check if killer is a player
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        // Add passive kill point
        pointSystem.addPassiveKill(killer.getUniqueId());

        // Send message to player
        int currentKills = pointSystem.getPassiveKills(killer.getUniqueId());
        messageSystem.sendPassiveKillMessage(killer, currentKills);
    }
}