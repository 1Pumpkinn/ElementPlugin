package hs.elementPlugin.listeners.items.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.managers.ElementManager;
import hs.elementPlugin.managers.ItemManager;
import hs.elementPlugin.util.ItemUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ElementItemUseListener implements Listener {
	private final ElementPlugin plugin;
	private final ElementManager elements;
	private final ItemManager itemManager;

	public ElementItemUseListener(ElementPlugin plugin, ElementManager elements, ItemManager itemManager) {
		this.plugin = plugin;
		this.elements = elements;
		this.itemManager = itemManager;
	}

	private boolean isElementItem(ItemStack stack) {
		return ItemUtil.isElementItem(plugin, stack);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(PlayerInteractEvent event) {
		ItemStack inHand = event.getItem();
		if (inHand != null && isElementItem(inHand)) {
			if (hs.elementPlugin.items.CoreConsumptionHandler.handleCoreConsume(event, plugin, elements)) return;
			itemManager.handleUse(event);
		}
	}
}


