package saturn.elementPlugin.listeners.items.handlers;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.util.ItemUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class ElementItemDropListener implements Listener {
	private final ElementPlugin plugin;

public ElementItemDropListener(ElementPlugin plugin) {
		this.plugin = plugin;
	}

	private boolean isElementItem(ItemStack stack) {
		return ItemUtil.isElementItem(plugin, stack);
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent event) {
		ItemStack stack = event.getItemDrop().getItemStack();
		if (isElementItem(stack)) {
			Player player = event.getPlayer();
			if (player.isDead() || player.getHealth() <= 0) {
			}
		}
	}
}


