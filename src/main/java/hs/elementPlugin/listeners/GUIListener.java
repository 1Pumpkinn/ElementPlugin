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
    // Prevent rapid re-open loops when inventories transition
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
            // Capture close reason to avoid reopening during inventory transitions
            org.bukkit.event.inventory.InventoryCloseEvent.Reason reason = event.getReason();
            // Delay the check to the next tick so element assignment can complete
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                // Skip if we just opened, or if a new inventory is opening/closed by plugin
                if (suppressReopen.contains(player.getUniqueId())) return;
                if (reason == org.bukkit.event.inventory.InventoryCloseEvent.Reason.OPEN_NEW ||
                        reason == org.bukkit.event.inventory.InventoryCloseEvent.Reason.PLUGIN) {
                    return;
                }
                hs.elementPlugin.managers.ElementManager em = plugin.getElementManager();
                if (em.data(player.getUniqueId()).getCurrentElement() == null) {
                    player.sendMessage(net.kyori.adventure.text.Component.text("You must choose an element to play!").color(net.kyori.adventure.text.format.NamedTextColor.RED));
                    suppressReopen.add(player.getUniqueId());
                    new hs.elementPlugin.gui.ElementSelectionGUI(plugin, player, false).open();
                    // Remove suppression shortly after to allow future legitimate closes to trigger reopen
                    plugin.getServer().getScheduler().runTaskLater(plugin, () -> suppressReopen.remove(player.getUniqueId()), 2L);
                }
            });
        }
    }
}