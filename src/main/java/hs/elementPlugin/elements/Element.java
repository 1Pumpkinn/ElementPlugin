package hs.elementPlugin.elements;

import org.bukkit.entity.Player;

public interface Element {
    ElementType getType();

    void applyUpsides(Player player, int upgradeLevel);

    boolean ability1(ElementContext context);

    boolean ability2(ElementContext context);

    void clearEffects(Player player);
}