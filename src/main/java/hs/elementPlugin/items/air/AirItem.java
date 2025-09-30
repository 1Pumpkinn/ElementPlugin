package hs.elementPlugin.items.air;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.items.ItemKeys;
import hs.elementPlugin.items.api.ElementItem;
import hs.elementPlugin.managers.ManaManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.List;

public final class AirItem implements ElementItem {
    public static final String RECIPE_KEY = "air_item";
    public static final String PROJ_KEY = "air_windcharge";

    @Override
    public ElementType getElementType() { return ElementType.AIR; }

    @Override
    public ItemStack create(ElementPlugin plugin) {
        ItemStack item = new ItemStack(Material.WIND_CHARGE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§fWind Charge (Air)");
        meta.setLore(List.of("§7Infinite", "§7On hit: Weakness I + Slowness II (10s)", "§7Cost: 75 mana"));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE, (byte)1);
        pdc.set(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING, ElementType.AIR.name());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void registerRecipe(ElementPlugin plugin) {
        ItemStack result = create(plugin);
        NamespacedKey key = new NamespacedKey(plugin, RECIPE_KEY);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape(" FE", "FWF", "EF ");
        recipe.setIngredient('F', Material.FEATHER);
        recipe.setIngredient('E', Material.ECHO_SHARD);
        recipe.setIngredient('W', Material.WIND_CHARGE);
        plugin.getServer().addRecipe(recipe);
    }

    @Override
    public boolean isItem(ItemStack stack, ElementPlugin plugin) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Byte b = pdc.get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_ITEM), PersistentDataType.BYTE);
        String t = pdc.get(new NamespacedKey(plugin, ItemKeys.KEY_ELEMENT_TYPE), PersistentDataType.STRING);
        return b != null && b == (byte)1 && ElementType.AIR.name().equals(t);
    }

    @Override
    public boolean handleUse(PlayerInteractEvent e, ElementPlugin plugin, ManaManager mana) {
        if (e.getHand() != EquipmentSlot.HAND && e.getHand() != EquipmentSlot.OFF_HAND) return false;
        Player p = e.getPlayer();
        ItemStack inHand = (e.getHand() == EquipmentSlot.HAND) ? p.getInventory().getItemInMainHand() : p.getInventory().getItemInOffHand();
        if (!isItem(inHand, plugin)) return false;

        int cost = 75;
        if (!mana.spend(p, cost)) {
            p.sendMessage("§cNot enough mana (75)");
            return true;
        }
        Snowball proj = p.launchProjectile(Snowball.class);
        proj.setVelocity(p.getLocation().getDirection().normalize().multiply(1.3));
        proj.getPersistentDataContainer().set(new NamespacedKey(plugin, PROJ_KEY), PersistentDataType.BYTE, (byte)1);
        proj.setShooter(p);
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BREEZE_SHOOT, 1f, 1.2f);
        p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1.2, 0), 10, 0.2, 0.2, 0.2, 0.01);
        e.setCancelled(true);
        return true;
    }

    @Override
    public void handleDamage(EntityDamageByEntityEvent e, ElementPlugin plugin) {
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity victim)) return;
        if (e.getDamager() instanceof org.bukkit.entity.Projectile proj) {
            Byte mark = proj.getPersistentDataContainer().get(new NamespacedKey(plugin, PROJ_KEY), PersistentDataType.BYTE);
            if (mark != null && mark == (byte)1) {
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WEAKNESS, 10 * 20, 0, true, true, true));
                victim.addPotionEffect(new org.bukkit.potion.PotionEffect(PotionEffectType.SLOWNESS, 10 * 20, 1, true, true, true));
            }
        }
    }
}
