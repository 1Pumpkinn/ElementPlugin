package hs.elementPlugin.listeners.player;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.gui.ElementSelectionGUI;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;
    private final ManaManager mana;

    public JoinListener(ElementPlugin plugin, ElementManager elements, ManaManager mana) {
        this.plugin = plugin;
        this.elements = elements;
        this.mana = mana;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Check if player has an element
        PlayerData pd = elements.data(p.getUniqueId());
        boolean first = (pd.getCurrentElement() == null);

        plugin.getLogger().info("Player " + p.getName() + " joined. Has element: " + !first);

        if (first) {
            plugin.getLogger().info("Opening element selection GUI for " + p.getName());
            // Open element selection GUI after a short delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        ElementSelectionGUI gui = new ElementSelectionGUI(plugin, p, false);
                        gui.open();
                        p.sendMessage(net.kyori.adventure.text.Component.text("Welcome! Please select your element.").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                        plugin.getLogger().info("GUI opened for " + p.getName());
                    }
                }
            }.runTaskLater(plugin, 20L); // 1 second delay
        } else {
            // CRITICAL FIX: Validate and clean up element effects on join
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        validateAndCleanupEffects(p);
                    }
                }
            }.runTaskLater(plugin, 10L); // Small delay to ensure player is fully loaded
        }

        // Ensure mana loaded
        mana.get(p.getUniqueId());
    }

    /**
     * CRITICAL: Validate that player only has their current element's effects
     * This fixes the bug where rerolling while logging out causes stacked effects
     */
    private void validateAndCleanupEffects(Player player) {
        PlayerData pd = elements.data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement == null) {
            plugin.getLogger().warning("Player " + player.getName() + " joined with no element assigned!");
            return;
        }

        // Step 1: Clear ALL element effects first (in case of stacking)
        for (ElementType type : ElementType.values()) {
            // Skip the current element - we'll apply those effects in step 2
            if (type == currentElement) continue;

            Element element = elements.get(type);
            if (element != null) {
                element.clearEffects(player);
            }
        }

        // Step 2: Ensure max health is correct
        if (currentElement != ElementType.LIFE) {
            // Reset to normal 20 HP if not Life element
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null && attr.getBaseValue() != 20.0) {
                attr.setBaseValue(20.0);
                // Cap health if it exceeds max
                if (!player.isDead() && player.getHealth() > 20.0) {
                    player.setHealth(20.0);
                }
            }
        } else {
            // Life element should have 30 HP
            var attr = player.getAttribute(Attribute.MAX_HEALTH);
            if (attr != null && attr.getBaseValue() != 30.0) {
                attr.setBaseValue(30.0);
                // Don't modify current health, just cap it if needed
                if (!player.isDead() && player.getHealth() > 30.0) {
                    player.setHealth(30.0);
                }
            }
        }

        // Step 3: Apply ONLY the current element's effects
        elements.applyUpsides(player);

        plugin.getLogger().info("Validated and cleaned up element effects for " + player.getName() + " (Element: " + currentElement + ")");
    }
}