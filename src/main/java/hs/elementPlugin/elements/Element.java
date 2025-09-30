package hs.elementPlugin.elements;

import hs.elementPlugin.managers.ConfigManager;
import hs.elementPlugin.managers.CooldownManager;
import hs.elementPlugin.managers.ManaManager;
import hs.elementPlugin.managers.TrustManager;
import org.bukkit.entity.Player;

public interface Element {
    ElementType getType();

    void applyUpsides(Player player, int upgradeLevel);

    boolean ability1(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config);

    boolean ability2(Player player, int upgradeLevel, ManaManager mana, CooldownManager cooldowns, TrustManager trust, ConfigManager config);
}