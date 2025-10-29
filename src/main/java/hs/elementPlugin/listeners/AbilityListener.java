package hs.elementPlugin.listeners;

import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityListener implements Listener {
    private final hs.elementPlugin.ElementPlugin plugin;
    private final ElementManager elements;

    // Track last offhand key press times for double-tap detection
    private final Map<UUID, Long> lastOffhandPress = new HashMap<>();

    // Tunable constants
    private static final long DOUBLE_TAP_THRESHOLD = 250; // ms (~5 ticks)
    private static final long CHECK_DELAY_TICKS = 6;      // wait before confirming single tap

    public AbilityListener(hs.elementPlugin.ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        // Make sure player has an element
        PlayerData pd = elements.data(id);
        ElementType element = pd.getCurrentElement();
        if (element == null) return;

        Long lastPress = lastOffhandPress.get(id);

        // --- Double-tap detection ---
        if (lastPress != null && (now - lastPress) <= DOUBLE_TAP_THRESHOLD) {
            // Second press detected within window -> allow normal swap
            lastOffhandPress.remove(id);

            // Clear again after short delay to prevent stale timing from staying
            new BukkitRunnable() {
                @Override
                public void run() {
                    lastOffhandPress.remove(id);
                }
            }.runTaskLater(plugin, 1); // 1 ticks = 50ms buffer

            return;
        }

        // First tap: store timestamp and wait briefly to see if a second tap comes
        lastOffhandPress.put(id, now);

        // Cancel the hand swap temporarily (we'll decide later whether to trigger ability)
        e.setCancelled(true);

        // Store whether player is sneaking NOW (not later when task runs)
        final boolean isSneaking = player.isSneaking();

        // Delay ability firing to confirm that this was not a double-tap
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check if this press is still valid (not cleared by a second tap)
                Long pressTime = lastOffhandPress.get(id);
                if (pressTime == null || !pressTime.equals(now)) return;

                // If player is offline, don't fire ability
                if (!player.isOnline()) {
                    lastOffhandPress.remove(id);
                    return;
                }

                CooldownManager cooldowns = plugin.getCooldownManager();
                String base = element.toString().toLowerCase();

                boolean abilityUsed = false;

                // Trigger ability
                if (isSneaking) {
                    if (!cooldowns.isOnCooldown(player, base + "_ability2")) {
                        elements.useAbility2(player);
                        abilityUsed = true;
                    }
                } else {
                    if (!cooldowns.isOnCooldown(player, base + "_ability1")) {
                        elements.useAbility1(player);
                        abilityUsed = true;
                    }
                }

                // Reset tracking so next press starts fresh
                lastOffhandPress.remove(id);

                // Optional small grace period to ensure clean reactivation
                if (abilityUsed) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            lastOffhandPress.remove(id);
                        }
                    }.runTaskLater(plugin, 2); // 2 ticks = 100ms
                }
            }
        }.runTaskLater(plugin, CHECK_DELAY_TICKS);
    }
}
