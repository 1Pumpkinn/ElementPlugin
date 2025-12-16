package saturn.elementPlugin.elements.impl.life;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.BaseElement;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.Ability;
import saturn.elementPlugin.elements.abilities.impl.life.TransfusionAbility;
import saturn.elementPlugin.elements.abilities.impl.life.LifeRegenAbility;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class LifeElement extends BaseElement {

    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    public LifeElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new LifeRegenAbility(plugin);
        this.ability2 = new TransfusionAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.LIFE;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Slower hunger drain (handled in LifeHungerListener)
        // No potion effect needed here

        // Upside 2: 15 Hearts (30 HP) instead of 20 HP
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) {
                player.setHealth(attr.getBaseValue());
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
        // No potion effects to remove anymore

        // Reset health to normal
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) {
                player.setHealth(20.0);
            }
        }

        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.GREEN + "Life";
    }

    @Override
    public String getDescription() {
        return "Masters of healing and growth. Life users have slower hunger drain and increased health.";
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