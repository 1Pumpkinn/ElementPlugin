package hs.elementPlugin.items.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.items.api.ElementItem;
import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class FireItem implements ElementItem {
    public static final String RECIPE_KEY = "fire_item";

    @Override
    public ElementType getElementType() { return ElementType.FIRE; }

    @Override
    public ItemStack create(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§cInferno Pick (Fire)");
        meta.setLore(List.of("§7Efficiency X", "§7Right-click: Haste V for 15s", "§7Cost: 75 mana"));
        meta.addEnchant(Enchantment.EFFICIENCY, 10, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE, (byte)1);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING, ElementType.FIRE.name());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void registerRecipe(ElementPlugin plugin) {
        ItemStack result = create(plugin);
        NamespacedKey key = new NamespacedKey(plugin, RECIPE_KEY);
        ShapedRecipe r = new ShapedRecipe(key, result);
        r.shape("NNN", " S ", " S ");
        r.setIngredient('N', Material.NETHERITE_INGOT);
        r.setIngredient('S', Material.STICK);
        plugin.getServer().addRecipe(r);
    }

    @Override
    public boolean isItem(ItemStack stack, ElementPlugin plugin) {
        if (stack == null || stack.getType() != Material.NETHERITE_PICKAXE || !stack.hasItemMeta()) return false;
        String t = stack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), org.bukkit.persistence.PersistentDataType.STRING);
        return ElementType.FIRE.name().equals(t);
    }

    @Override
    public boolean handleUse(PlayerInteractEvent e, ElementPlugin plugin, ManaManager mana, ConfigManager config) {
        if (e.getHand() != EquipmentSlot.HAND) return false;
        var a = e.getAction();
        if (!(a == org.bukkit.event.block.Action.RIGHT_CLICK_AIR || a == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) return false;
        var p = e.getPlayer();
        var inMain = p.getInventory().getItemInMainHand();
        if (!isItem(inMain, plugin)) return false;
        int cost = config.getItemUseCost(ElementType.FIRE);
        if (!mana.spend(p, cost)) { p.sendMessage("§cNot enough mana (" + cost + ")"); return true; }
        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 15 * 20, 4, true, true, true));
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
        e.setCancelled(true);
        return true;
    }

    @Override
    public void handleDamage(EntityDamageByEntityEvent e, ElementPlugin plugin) {
        // No special on-hit effect for this item
    }
}