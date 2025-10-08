package hs.elementPlugin.listeners;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.earth.EarthElement;
import hs.elementPlugin.managers.ElementManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.EnumSet;
import java.util.Set;

public class EarthElementListener implements Listener {
    private final ElementManager elements;
    private final ElementPlugin plugin;

    public EarthElementListener(ElementManager elements, ElementPlugin plugin) {
        this.elements = elements;
        this.plugin = plugin;
    }

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

        // Check if mob can be charmed (prevent boss mobs)
        if (mob instanceof Wither || mob instanceof EnderDragon || mob instanceof Warden) {
            p.sendMessage(ChatColor.RED + "This creature cannot be charmed!");
            e.setCancelled(true);
            return;
        }

        // Consume the ability
        p.removeMetadata(EarthElement.META_CHARM_NEXT_UNTIL, plugin);

        long expire = System.currentTimeMillis() + 30_000L;
        mob.setMetadata("earth_charmed_owner", new FixedMetadataValue(plugin, p.getUniqueId().toString()));
        mob.setMetadata("earth_charmed_until", new FixedMetadataValue(plugin, expire));

        p.sendMessage(ChatColor.GREEN + "Mob charmed! It will follow you for 30s.");
        e.setCancelled(true);
    }
}