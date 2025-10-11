package hs.elementPlugin.listeners;

import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AbilityListener implements Listener {
    private final hs.elementPlugin.ElementPlugin plugin;
    private final ElementManager elements;
    
    // Track last offhand key press times for double-tap detection
    private final Map<UUID, Long> lastOffhandPress = new HashMap<>();
    private static final long DOUBLE_TAP_THRESHOLD = 500; // 500ms for double-tap detection

    public AbilityListener(hs.elementPlugin.ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }


    // Handle abilities using the offhand keybind (F key by default)
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Check for double-tap
        boolean isDoubleTap = false;
        if (lastOffhandPress.containsKey(playerId)) {
            long timeSinceLastPress = currentTime - lastOffhandPress.get(playerId);
            if (timeSinceLastPress <= DOUBLE_TAP_THRESHOLD) {
                isDoubleTap = true;
            }
        }
        
        // Update last press time
        lastOffhandPress.put(playerId, currentTime);
        
        // Check if player has an element
        PlayerData pd = elements.data(playerId);
        ElementType currentElement = pd.getCurrentElement();
        if (currentElement == null) {
            // No element, allow normal offhand swapping
            e.setCancelled(false);
            return;
        }
        
        // If double-tap, allow normal offhand swapping regardless of cooldowns
        if (isDoubleTap) {
            plugin.getLogger().info("Player " + player.getName() + " double-tapped offhand - allowing normal swap");
            e.setCancelled(false);
            return;
        }
        
        // Check for ability cooldowns
        CooldownManager cooldownManager = plugin.getCooldownManager();
        String ability1Key = currentElement.toString().toLowerCase() + "_ability1";
        String ability2Key = currentElement.toString().toLowerCase() + "_ability2";
        
        boolean ability1OnCooldown = cooldownManager.isOnCooldown(player, ability1Key);
        boolean ability2OnCooldown = cooldownManager.isOnCooldown(player, ability2Key);
        
        if (player.isSneaking()) {
            // Ability 2: Shift + Offhand keybind (Shift + F)
            if (ability2OnCooldown) {
                // Ability 2 is on cooldown, prevent any offhand movement
                plugin.getLogger().info("Player " + player.getName() + " tried to use ability 2 while on cooldown - preventing offhand swap");
                e.setCancelled(true);
                return;
            }
            boolean ok = elements.useAbility2(player);
            e.setCancelled(true); // Always cancel for ability use
        } else {
            // Ability 1: Offhand keybind (F key)
            if (ability1OnCooldown) {
                // Ability 1 is on cooldown, prevent any offhand movement
                plugin.getLogger().info("Player " + player.getName() + " tried to use ability 1 while on cooldown - preventing offhand swap");
                e.setCancelled(true);
                return;
            }
            boolean ok = elements.useAbility1(player);
            e.setCancelled(true); // Always cancel for ability use
        }
    }
}
