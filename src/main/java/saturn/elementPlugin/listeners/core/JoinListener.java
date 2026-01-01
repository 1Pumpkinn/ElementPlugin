package saturn.elementPlugin.listeners.core;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import saturn.elementPlugin.managers.ManaManager;
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
        boolean hasElement = (pd.getCurrentElement() != null);

        plugin.getLogger().info("Player " + p.getName() + " joined. Has element: " + hasElement);

        if (!hasElement) {
            // UPDATED: No longer auto-assign element - player must use a Reroller
            plugin.getLogger().info(p.getName() + " has no element - they must use a Reroller to get one");

            // Send a welcome message explaining they need a Reroller (only on first join)
            if (!p.hasPlayedBefore()) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (p.isOnline()) {
                            p.sendMessage(net.kyori.adventure.text.Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
                            p.sendMessage(net.kyori.adventure.text.Component.text("Welcome to Element Plugin!")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                                    .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD));
                            p.sendMessage(net.kyori.adventure.text.Component.empty());
                            p.sendMessage(net.kyori.adventure.text.Component.text("You don't have an element yet.")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.YELLOW));
                            p.sendMessage(net.kyori.adventure.text.Component.text("Craft or obtain a ")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)
                                    .append(net.kyori.adventure.text.Component.text("Reroller")
                                            .color(net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE)
                                            .decorate(net.kyori.adventure.text.format.TextDecoration.BOLD))
                                    .append(net.kyori.adventure.text.Component.text(" to get your first element!")
                                            .color(net.kyori.adventure.text.format.NamedTextColor.GRAY)));
                            p.sendMessage(net.kyori.adventure.text.Component.empty());
                            p.sendMessage(net.kyori.adventure.text.Component.text("ℹ Use ")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)
                                    .append(net.kyori.adventure.text.Component.text("/elements <name>")
                                            .color(net.kyori.adventure.text.format.NamedTextColor.WHITE))
                                    .append(net.kyori.adventure.text.Component.text(" to learn about elements")
                                            .color(net.kyori.adventure.text.format.NamedTextColor.AQUA)));
                            p.sendMessage(net.kyori.adventure.text.Component.text("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    .color(net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY));
                        }
                    }
                }.runTaskLater(plugin, 40L); // 2 second delay for better visibility
            }
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