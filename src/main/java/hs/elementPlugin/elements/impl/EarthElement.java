package hs.elementPlugin.elements.impl;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EarthElement extends BaseElement {
    public static final String META_MINE_UNTIL = "earth_mine_until";
    public static final String META_CHARM_NEXT_UNTIL = "earth_charm_next_until";

    public EarthElement(ElementPlugin plugin) {
        super(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.EARTH; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Hero of the Village I
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();
        long until = System.currentTimeMillis() + 20_000L;
        player.setMetadata(META_MINE_UNTIL, new FixedMetadataValue(plugin, until));
        player.sendMessage(ChatColor.GREEN + "Earth shaping active for 20s (3x3 mining)");
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 0.8f);
        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        long until = System.currentTimeMillis() + 30_000L;
        player.setMetadata(META_CHARM_NEXT_UNTIL, new FixedMetadataValue(plugin, until));
        player.sendMessage(ChatColor.GOLD + "Punch a mob to charm it for 30s");
        return true;
    }
}