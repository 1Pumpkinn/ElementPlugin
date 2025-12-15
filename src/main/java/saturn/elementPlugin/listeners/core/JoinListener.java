package saturn.elementPlugin.listeners.core;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import saturn.elementPlugin.managers.ManaManager;
import saturn.elementPlugin.managers.TrustManager;
import saturn.elementPlugin.util.SmartEffectCleaner;
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
    private final TrustManager trust;

    public JoinListener(ElementPlugin plugin, ElementManager elements, ManaManager mana, TrustManager trust) {
        this.plugin = plugin;
        this.elements = elements;
        this.mana = mana;
        this.trust = trust;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Restore tab list for teams
        trust.handlePlayerJoin(p);

        // Check if player has an element
        PlayerData pd = elements.data(p.getUniqueId());
        boolean first = (pd.getCurrentElement() == null);

        plugin.getLogger().info("Player " + p.getName() + " joined. Has element: " + !first);

        if (first) {
            plugin.getLogger().info("Assigning random element to " + p.getName());
            // Automatically roll and assign a random element after a short delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        elements.rollAndAssign(p);
                        p.sendMessage(net.kyori.adventure.text.Component.text("Welcome! You have been assigned a random element.").color(net.kyori.adventure.text.format.NamedTextColor.GREEN));
                        plugin.getLogger().info("Element assigned to " + p.getName());
                    }
                }
            }.runTaskLater(plugin, 20L); // 1 second delay
        } else {
            // Validate effects on join
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

    private void validateAndCleanupEffects(Player player) {
        PlayerData pd = elements.data(player.getUniqueId());
        ElementType currentElement = pd.getCurrentElement();

        if (currentElement == null) {
            plugin.getLogger().warning("Player " + player.getName() + " joined with no element assigned!");
            return;
        }

        // Use SmartEffectCleaner to remove invalid infinite effects
        SmartEffectCleaner.cleanInvalidInfiniteEffects(plugin, player);

        // Apply current element effects to ensure everything is correct
        elements.applyUpsides(player);

        plugin.getLogger().info("Validated element effects for " + player.getName() + " (Element: " + currentElement + ")");
    }
}