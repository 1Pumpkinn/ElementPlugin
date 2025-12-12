package hs.elementPlugin.elements.impl.water;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.impl.water.WaterPrisonAbility;
import hs.elementPlugin.elements.abilities.impl.water.WaterWhirlpoolAbility;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class WaterElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    public WaterElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new WaterWhirlpoolAbility(plugin);
        this.ability2 = new WaterPrisonAbility(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.WATER; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Conduit power (permanent water breathing)
        player.addPotionEffect(new PotionEffect(PotionEffectType.CONDUIT_POWER, Integer.MAX_VALUE, 0, true, false));

        // Upside 2: Mine faster in water - requires upgrade level 2
        if (upgradeLevel >= 2) {
            var attr = player.getAttribute(org.bukkit.attribute.Attribute.SUBMERGED_MINING_SPEED);
            if (attr != null) {
                // Default is 0.2 (5x slower underwater)
                // Set to 1.2 (slightly faster than on land)
                attr.setBaseValue(1.2);
            }
        }
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        return ability1.execute(context);
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        return ability2.execute(context);
    }

    @Override
    public void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.CONDUIT_POWER);

        // Reset underwater mining speed to default
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.SUBMERGED_MINING_SPEED);
        if (attr != null) {
            attr.setBaseValue(0.2); // Default underwater speed
        }

        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.AQUA + "Water";
    }

    @Override
    public String getDescription() {
        return "Harness the flowing power of water to control the battlefield.";
    }

    @Override
    public String getAbility1Name() {
        return ability1.getName();
    }

    @Override
    public String getAbility1Description() {
        return ability1.getDescription();
    }

    @Override
    public String getAbility2Name() {
        return ability2.getName();
    }

    @Override
    public String getAbility2Description() {
        return ability2.getDescription();
    }
}