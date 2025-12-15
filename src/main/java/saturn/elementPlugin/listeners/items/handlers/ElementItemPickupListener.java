package saturn.elementPlugin.listeners.items.handlers;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.managers.ElementManager;
import saturn.elementPlugin.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class ElementItemPickupListener implements Listener {
	private final ElementPlugin plugin;
	private final ElementManager elements;

	public ElementItemPickupListener(ElementPlugin plugin, ElementManager elements) {
		this.plugin = plugin;
		this.elements = elements;
	}

	@EventHandler
	public void onPickup(EntityPickupItemEvent event) {
		if (!(event.getEntity() instanceof Player player)) return;
		ItemStack stack = event.getItem().getItemStack();
		if (!ItemUtil.isElementItem(plugin, stack)) return;
		ElementType type = ItemUtil.getElementType(plugin, stack);
		if (type == null) return;

		if (type == ElementType.LIFE || type == ElementType.DEATH) {
			return;
		}

		PlayerData playerData = elements.data(player.getUniqueId());
		ElementType oldElement = playerData.getCurrentElement();
		if (oldElement != type) {
			elements.setElement(player, type);
		}
	}
}


