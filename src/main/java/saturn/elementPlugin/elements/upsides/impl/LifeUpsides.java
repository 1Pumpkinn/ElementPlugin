package saturn.elementPlugin.elements.upsides.impl;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.upsides.BaseUpsides;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class LifeUpsides extends BaseUpsides {

    public LifeUpsides(ElementManager elementManager) {
        super(elementManager);
    }

    @Override
    public ElementType getElementType() {
        return ElementType.LIFE;
    }

    /**
     * Apply all Life element upsides to a player
     * Upside 1: Slower hunger drain (15% slower) - handled in LifeHungerListener
     * Upside 2: 15 Hearts (30 HP) instead of 20 HP
     *
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Life element
     */
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Slower hunger drain (handled in LifeHungerListener, no potion effect needed)

        // Upside 2: 15 Hearts (30 HP) instead of 20 HP
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) player.setHealth(attr.getBaseValue());
        }
    }

    /**
     * Reset player's max health to default (20 HP)
     * @param player The player to reset
     */
    public void resetMaxHealth(Player player) {
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) player.setHealth(20.0);
        }
    }
}