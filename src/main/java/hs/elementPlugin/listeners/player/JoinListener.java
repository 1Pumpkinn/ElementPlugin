package hs.elementPlugin.listeners.player;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.gui.ElementSelectionGUI;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Check if player has an element
        boolean first = (elements.data(p.getUniqueId()).getCurrentElement() == null);

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
            // Cap health to max health on join (in case they logged out with Life element)
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (p.isOnline()) {
                        var attr = p.getAttribute(Attribute.MAX_HEALTH);
                        if (attr != null && p.getHealth() > attr.getBaseValue()) {
                            p.setHealth(attr.getBaseValue());
                        }
                    }
                }
            }.runTaskLater(plugin, 5L); // Small delay to ensure health loads
        }

        // Ensure mana loaded
        mana.get(p.getUniqueId());
    }
}