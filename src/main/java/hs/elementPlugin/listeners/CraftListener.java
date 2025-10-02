package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class CraftListener implements Listener {
    private final ElementPlugin plugin;
    private final ElementManager elements;

    public CraftListener(ElementPlugin plugin, ElementManager elements) {
        this.plugin = plugin;
        this.elements = elements;
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player p)) return;
        ItemStack result = e.getRecipe() == null ? null : e.getRecipe().getResult();
        if (result == null) return;
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        // Upgrader crafting
        Integer level = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, ItemKeys.KEY_UPGRADER_LEVEL), PersistentDataType.INTEGER);
        if (level != null) {
            PlayerData pd = elements.data(p.getUniqueId());
            ElementType type = pd.getCurrentElement();
            if (type == null) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + "You don't have an element yet.");
                return;
            }
            int current = pd.getUpgradeLevel(type);
            if (level <= current) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.YELLOW + "You already have this upgrade.");
                return;
            }

            // Allow the craft to consume materials normally
            pd.setUpgradeLevel(type, level);
            plugin.getDataStore().save(pd);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            if (level == 1) {
                p.sendMessage(ChatColor.GREEN + "Unlocked Ability 1 for " + ChatColor.AQUA + type.name());
            } else if (level == 2) {
                p.sendMessage(ChatColor.AQUA + "Unlocked Ability 2 and Upside 2 for " + ChatColor.GOLD + type.name());
                // Reapply upsides to include Upside 2
                elements.applyUpsides(p);
            }
            return;
        }

        // Element item crafting: enforce once per player per element type
        Byte isElem = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE);
        if (isElem != null && isElem == (byte)1) {
            String t = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING);
            ElementType type;
            try { type = ElementType.valueOf(t); } catch (Exception ex) { return; }
            PlayerData pd = elements.data(p.getUniqueId());
            if (pd.hasElementItem(type)) {
                e.setCancelled(true);
                p.sendMessage(ChatColor.RED + "You can only craft this item once.");
                return;
            }

            // Allow the craft to consume materials
            pd.addElementItem(type);
            // Reset ALL upgrade levels when crafting a new element item
            for (ElementType et : ElementType.values()) {
                pd.setUpgradeLevel(et, 0);
            }
            plugin.getDataStore().save(pd);
            p.playSound(p.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);
            p.sendMessage(ChatColor.GREEN + "Crafted element item for " + ChatColor.AQUA + type.name());
            p.sendMessage(ChatColor.YELLOW + "All upgrades reset to None");
        }
    }
}