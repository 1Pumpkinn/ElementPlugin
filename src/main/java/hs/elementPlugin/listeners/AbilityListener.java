package hs.elementPlugin.listeners;

import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AbilityListener implements Listener {
    private static final long DOUBLE_TAP_THRESHOLD_MS = 250;
    private static final long CHECK_DELAY_TICKS = 6;
    private static final long CLEANUP_DELAY_TICKS = 2;

    private final hs.elementPlugin.ElementPlugin plugin;
    private final ElementManager elements;
    private final Map<UUID, TapTracker> tapTrackers = new ConcurrentHashMap<>();

    public AbilityListener(hs.elementPlugin.ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();

        // Verify player has an element
        if (!hasElement(player)) return;

        UUID playerId = player.getUniqueId();
        TapTracker tracker = tapTrackers.computeIfAbsent(playerId, k -> new TapTracker());

        long currentTime = System.currentTimeMillis();

        // Check for double-tap
        if (tracker.isDoubleTap(currentTime)) {
            tracker.reset();
            scheduleCleanup(playerId);
            return; // Allow normal swap
        }

        // First tap - cancel and schedule ability check
        event.setCancelled(true);
        tracker.recordTap(currentTime, player.isSneaking());

        scheduleAbilityActivation(player, playerId, currentTime);
    }

    private boolean hasElement(Player player) {
        PlayerData pd = elements.data(player.getUniqueId());
        return pd.getCurrentElement() != null;
    }

    private void scheduleAbilityActivation(Player player, UUID playerId, long tapTime) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    tapTrackers.remove(playerId);
                    return;
                }

                TapTracker tracker = tapTrackers.get(playerId);
                if (tracker == null || !tracker.isValidTap(tapTime)) {
                    return; // Second tap occurred, cancelled
                }

                // Execute ability
                boolean success = tracker.wasShiftHeld ?
                        elements.useAbility2(player) :
                        elements.useAbility1(player);

                if (success) {
                    scheduleCleanup(playerId);
                }
            }
        }.runTaskLater(plugin, CHECK_DELAY_TICKS);
    }

    private void scheduleCleanup(UUID playerId) {
        new BukkitRunnable() {
            @Override
            public void run() {
                tapTrackers.remove(playerId);
            }
        }.runTaskLater(plugin, CLEANUP_DELAY_TICKS);
    }

    /**
     * Tracks tap timing and state for double-tap detection
     */
    private static class TapTracker {
        private long lastTapTime = 0;
        private boolean wasShiftHeld = false;

        boolean isDoubleTap(long currentTime) {
            return lastTapTime > 0 && (currentTime - lastTapTime) <= DOUBLE_TAP_THRESHOLD_MS;
        }

        void recordTap(long time, boolean shiftHeld) {
            this.lastTapTime = time;
            this.wasShiftHeld = shiftHeld;
        }

        boolean isValidTap(long originalTime) {
            return lastTapTime == originalTime;
        }

        void reset() {
            lastTapTime = 0;
            wasShiftHeld = false;
        }
    }
}