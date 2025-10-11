package hs.elementPlugin.elements.impl.fire;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.impl.fire.FireBreathAbility;
import hs.elementPlugin.elements.abilities.impl.fire.FireSummonAbility;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireElement extends BaseElement {
    private final Ability ability1;
    private final Ability ability2;

    public FireElement(ElementPlugin plugin) {
        super(plugin);
        this.ability1 = new FireBreathAbility(plugin);
        this.ability2 = new FireSummonAbility(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.FIRE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));
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
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);
        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.RED + "Fire";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Masters of flame and heat. Fire users are immune to fire damage and can breathe fire or summon blazes.";
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