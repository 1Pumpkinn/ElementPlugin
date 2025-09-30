package hs.elementPlugin.elements.impl;

import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class LifeElement implements Element {
    @Override
    public ElementType getType() { return ElementType.LIFE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: 15 hearts
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) player.setHealth(attr.getBaseValue());
        }
    }

    @Override
    public boolean ability1(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 1) { player.sendMessage(ChatColor.RED + "Need Upgrade I"); return false; }
        int cost = config.getAbility1Cost(ElementType.LIFE);
        if (!mana.spend(player, cost)) { player.sendMessage(ChatColor.RED + "Not enough mana ("+cost+")"); return false; }
        int radius = 5;
        for (Player other : player.getWorld().getNearbyPlayers(player.getLocation(), radius)) {
            if (!other.equals(player) && !trust.isTrusted(player.getUniqueId(), other.getUniqueId())) continue;
            other.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, true, true, true));
        }
        player.sendMessage(ChatColor.GREEN + "Regen aura applied");
        return true;
    }

    @Override
    public boolean ability2(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 2) { player.sendMessage(ChatColor.RED + "Need Upgrade II"); return false; }
        int cost = config.getAbility2Cost(ElementType.LIFE);
        if (!mana.spend(player, cost)) { player.sendMessage(ChatColor.RED + "Not enough mana ("+cost+")"); return false; }
        // Grow crops in 5x5 around player
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block b = player.getLocation().add(dx, 0, dz).getBlock();
                // Check current and one above (for crops on farmland at Y)
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