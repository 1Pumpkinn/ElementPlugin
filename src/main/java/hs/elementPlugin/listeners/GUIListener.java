package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.gui.ElementSelectionGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class GUIListener implements Listener {
    private final ElementPlugin plugin;
    private final java.util.Set<java.util.UUID> suppressReopen = new java.util.HashSet<>();

    public GUIListener(ElementPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (title.contains("Select Your Element")) {
            event.setCancelled(true);

            ElementSelectionGUI gui = ElementSelectionGUI.getGUI(player.getUniqueId());
            if (gui != null) {
                gui.handleClick(event.getRawSlot());
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (title.contains("Select Your Element")) {
            ElementSelectionGUI.removeGUI(player.getUniqueId());

            if (!plugin.getConfigManager().isForceElementSelection()) {
                return;
            }

            InventoryCloseEvent.Reason reason = event.getReason();
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (suppressReopen.contains(player.getUniqueId())) return;
                if (reason == InventoryCloseEvent.Reason.OPEN_NEW ||
                        reason == InventoryCloseEvent.Reason.PLUGIN) {
                    return;
                }

                var elementManager = plugin.getElementManager();
                if (elementManager.data(player.getUniqueId()).getCurrentElement() == null) {
                    if (plugin.getConfigManager().isReopenOnCloseWithoutSelection()) {
                        player.sendMessage(net.kyori.adventure.text.Component.text("You must choose an element to play!")
                                .color(net.kyori.adventure.text.format.NamedTextColor.RED));
                        suppressReopen.add(player.getUniqueId());
                        new ElementSelectionGUI(plugin, player, false).open();
                        plugin.getServer().getScheduler().runTaskLater(plugin,
                                () -> suppressReopen.remove(player.getUniqueId()), 2L);
                    }
                }
            });
        }
    }
}