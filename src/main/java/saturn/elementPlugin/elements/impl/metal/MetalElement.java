package saturn.elementPlugin.elements.impl.metal;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.BaseElement;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.Ability;
import saturn.elementPlugin.elements.abilities.impl.metal.MetalDashAbility;
import saturn.elementPlugin.elements.abilities.impl.metal.MetalChainAbility;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MetalElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    public MetalElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new MetalDashAbility(plugin);
        this.ability2 = new MetalChainAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.METAL;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Haste 1 permanently
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, 0, true, false));

        // Upside 2: Reduced knockback (handled in MetalKnockbackListener) - requires upgrade level 2
        // No potion effect needed here
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
        player.removePotionEffect(PotionEffectType.HASTE);
        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.GRAY + "Metal";
    }

    @Override
    public String getDescription() {
        return "Masters of chains and iron. Metal users mine faster and resist knockback.";
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