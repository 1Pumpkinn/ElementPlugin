package saturn.elementPlugin.elements.upsides.impl;

import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.upsides.BaseUpsides;
import saturn.elementPlugin.managers.ElementManager;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
     * @param player The player to apply upsides to
     * @param upgradeLevel The player's upgrade level for Life element
     */
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Regeneration I permanently
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false));

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