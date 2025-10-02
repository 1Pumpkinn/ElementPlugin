package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LifeElement extends BaseElement {

    public LifeElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.LIFE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: 15 hearts
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) player.setHealth(attr.getBaseValue());
        }
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        int radius = 5;
        for (Player other : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
            if (!other.equals(player) && !context.getTrustManager().isTrusted(player.getUniqueId(), other.getUniqueId())) continue;
            other.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, true, true, true));
        }
        player.sendMessage(ChatColor.GREEN + "Regen aura applied");
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        // Grow crops in 5x5 around player
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block b = player.getLocation().add(dx, 0, dz).getBlock();
                growIfCrop(b);
                growIfCrop(b.getRelative(0, 1, 0));
            }
        }
        player.sendMessage(ChatColor.GOLD + "Crops surged with life!");
        return true;
    }

    private void growIfCrop(Block b) {
        if (b == null) return;
        var data = b.getBlockData();
        if (data instanceof Ageable ageable) {
            ageable.setAge(ageable.getMaximumAge());
            b.setBlockData(ageable, true);
        }
    }
}