package hs.elementPlugin.items.water;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.items.api.ElementItem;
import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public class WaterItem implements ElementItem {
    public static final String RECIPE_KEY = "water_item";
    public static final String PROJ_KEY = "water_trident";

    @Override
    public ElementType getElementType() { return ElementType.WATER; }

    @Override
    public ItemStack create(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§bTidecaller Trident (Water)");
        meta.setLore(List.of("§7Loyalty X", "§7On hit: Mining Fatigue I (5s)", "§7Cost to throw: mana"));
        meta.addEnchant(Enchantment.LOYALTY, 10, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE, (byte)1);
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING, ElementType.WATER.name());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void registerRecipe(ElementPlugin plugin) {
        ItemStack result = create(plugin);
        NamespacedKey key = new NamespacedKey(plugin, RECIPE_KEY);
        ShapedRecipe r = new ShapedRecipe(key, result);
        r.shape("PCP", "PTP", "PCP");
        r.setIngredient('P', Material.PRISMARINE_CRYSTALS);
        r.setIngredient('C', Material.PRISMARINE_SHARD);
        r.setIngredient('T', Material.HEART_OF_THE_SEA);
        plugin.getServer().addRecipe(r);
    }

    @Override
    public boolean isItem(ItemStack stack, ElementPlugin plugin) {
        if (stack == null || stack.getType() != Material.TRIDENT || !stack.hasItemMeta()) return false;
        String t = stack.getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING);
        return ElementType.WATER.name().equals(t);
    }

    @Override
    public boolean handleUse(PlayerInteractEvent e, ElementPlugin plugin, ManaManager mana, ConfigManager config) {
        // Let vanilla throw action proceed; cost is handled in handleLaunch
        return false;
    }

    @Override
    public void handleDamage(EntityDamageByEntityEvent e, ElementPlugin plugin) {
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity victim)) return;
        if (e.getDamager() instanceof Trident tr) {
            Byte mark = tr.getPersistentDataContainer().get(new NamespacedKey(plugin, PROJ_KEY), PersistentDataType.BYTE);
            if (mark != null && mark == (byte)1) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_DIGGING, 5 * 20, 0, true, true, true));
            }
        }
    }

    @Override
    public void handleLaunch(ProjectileLaunchEvent e, ElementPlugin plugin, ManaManager mana, ConfigManager config) {
        if (!(e.getEntity() instanceof Trident tr)) return;
        if (!(tr.getShooter() instanceof Player p)) return;
        ItemStack inMain = p.getInventory().getItemInMainHand();
        ItemStack inOff = p.getInventory().getItemInOffHand();
        if (!isItem(inMain, plugin) && !isItem(inOff, plugin)) return;

        int cost = config.getItemThrowCost(ElementType.WATER);
        if (!mana.spend(p, cost)) {
            // Cancel throw if not enough mana
            e.setCancelled(true);
            p.sendMessage("§cNot enough mana (" + cost + ")");
            return;
        }
        tr.getPersistentDataContainer().set(new NamespacedKey(plugin, PROJ_KEY), PersistentDataType.BYTE, (byte)1);
    }
}