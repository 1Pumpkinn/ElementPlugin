package hs.elementPlugin.listeners;

import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.impl.EarthElement;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumSet;
import java.util.Set;

public class EarthListener implements Listener {
    private final ElementManager elements;

    public EarthListener(ElementManager elements) {
        this.elements = elements;
    }

    private static final Set<Material> STONE_TYPES = EnumSet.of(Material.STONE, Material.DEEPSLATE);

    private static final Set<Material> ORES = EnumSet.of(
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
            Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE
    );

    @EventHandler
    public void onDrop(BlockDropItemEvent e) {
        Player p = e.getPlayer();
        var pd = elements.data(p.getUniqueId());
        if (pd.getCurrentElement() != ElementType.EARTH || pd.getUpgradeLevel(ElementType.EARTH) < 2) return;
        if (!ORES.contains(e.getBlockState().getType())) return;
        e.getItems().forEach(item -> item.getItemStack().setAmount(item.getItemStack().getAmount() * 2));
    }

    @EventHandler
    public void onPunch(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!(e.getEntity() instanceof Mob mob)) return;
        if (!p.hasMetadata(EarthElement.META_CHARM_NEXT_UNTIL)) return;

        long until = p.getMetadata(EarthElement.META_CHARM_NEXT_UNTIL).get(0).asLong();
        if (System.currentTimeMillis() > until) return;

        // Consume the ability
        p.removeMetadata(EarthElement.META_CHARM_NEXT_UNTIL, hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class));

        long expire = System.currentTimeMillis() + 30_000L;
        mob.setMetadata("earth_charmed_owner", new org.bukkit.metadata.FixedMetadataValue(
                hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class),
                p.getUniqueId().toString()));
        mob.setMetadata("earth_charmed_until", new org.bukkit.metadata.FixedMetadataValue(
                hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class),
                expire));

        p.sendMessage(org.bukkit.ChatColor.GREEN + "Mob charmed! It will follow you for 30s.");
        e.setCancelled(true);
    }
}