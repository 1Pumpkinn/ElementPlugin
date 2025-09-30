package hs.elementPlugin.elements.impl;

import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EarthElement implements Element {
    public static final String META_MINE_UNTIL = "earth_mine_until";
    public static final String META_CHARM_NEXT_UNTIL = "earth_charm_next_until";

    @Override
    public ElementType getType() { return ElementType.EARTH; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Hero of the Village I
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    public boolean ability1(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 1) { player.sendMessage(ChatColor.RED + "Need Upgrade I"); return false; }
        int cost = config.getAbility1Cost(ElementType.EARTH);
        if (!mana.spend(player, cost)) { player.sendMessage(ChatColor.RED + "Not enough mana ("+cost+")"); return false; }
        long until = System.currentTimeMillis() + 20_000L;
        player.setMetadata(META_MINE_UNTIL, new FixedMetadataValue(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), until));
        player.sendMessage(ChatColor.GREEN + "Earth shaping active for 20s (3x3 mining)");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);
        return true;
    }

    @Override
    public boolean ability2(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config) {
        if (upgradeLevel < 2) { player.sendMessage(ChatColor.RED + "Need Upgrade II"); return false; }
        int cost = config.getAbility2Cost(ElementType.EARTH);
        if (!mana.spend(player, cost)) { player.sendMessage(ChatColor.RED + "Not enough mana ("+cost+")"); return false; }
        long until = System.currentTimeMillis() + 30_000L;
        player.setMetadata(META_CHARM_NEXT_UNTIL, new FixedMetadataValue(hs.elementPlugin.ElementPlugin.getPlugin(hs.elementPlugin.ElementPlugin.class), until));
        player.sendMessage(ChatColor.GOLD + "Punch a mob to charm it for 30s");
        return true;
    }
}