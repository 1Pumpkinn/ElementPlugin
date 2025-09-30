package hs.elementPlugin.items.earth;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.items.api.ElementItem;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class EarthItem implements ElementItem {
    public static final String RECIPE_KEY = "earth_item";

    @Override
    public ElementType getElementType() {
        return ElementType.EARTH;
    }

    @Override
    public ItemStack create(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.CARROT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§6Nature's Bounty (Earth)");
        meta.setLore(List.of(
                "§7Infinite",
                "§7Right-click: Absorption 10 hearts + Regen V (5s)",
                "§7Cost: 75 mana"
        ));
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM),
                PersistentDataType.BYTE,
                (byte)1
        );
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE),
                PersistentDataType.STRING,
                ElementType.EARTH.name()
        );
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void registerRecipe(ElementPlugin plugin) {
        ItemStack result = create(plugin);
        NamespacedKey key = new NamespacedKey(plugin, RECIPE_KEY);
        ShapedRecipe r = new ShapedRecipe(key, result);
        r.shape("GDG", "DCD", "GDG");
        r.setIngredient('G', Material.GOLDEN_CARROT);
        r.setIngredient('D', Material.DIRT);
        r.setIngredient('C', Material.CARROT);
        plugin.getServer().addRecipe(r);
    }

    @Override
    public boolean isItem(ItemStack stack, ElementPlugin plugin) {
        if (stack == null || stack.getType() != Material.CARROT || !stack.hasItemMeta()) {
            return false;
        }
        String t = stack.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE),
                PersistentDataType.STRING
        );
        return ElementType.EARTH.name().equals(t);
    }

    @Override
    public boolean handleUse(PlayerInteractEvent e, ElementPlugin plugin, ManaManager mana) {
        if (e.getHand() != EquipmentSlot.HAND) return false;

        var a = e.getAction();
        if (!(a == org.bukkit.event.block.Action.RIGHT_CLICK_AIR ||
                a == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK)) {
            return false;
        }

        Player p = e.getPlayer();
        ItemStack inMain = p.getInventory().getItemInMainHand();

        if (!isItem(inMain, plugin)) return false;

        int cost = 75;
        if (!mana.spend(p, cost)) {
            p.sendMessage("§cNot enough mana (75)");
            return true;
        }

        // Give 10 absorption hearts (20 health points)
        p.addPotionEffect(new PotionEffect(
                PotionEffectType.ABSORPTION,
                5 * 20,  // 5 seconds
                9,       // Level 10 (amplifier is level-1)
                true,
                true,
                true
        ));

        // Give Regeneration V for 5 seconds
        p.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                5 * 20,  // 5 seconds
                4,       // Level 5 (amplifier is level-1)
                true,
                true,
                true
        ));

        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        e.setCancelled(true);
        return true;
    }

    @Override
    public void handleDamage(EntityDamageByEntityEvent e, ElementPlugin plugin) {
        // No special on-hit effect for this item
    }
}