package saturn.elementPlugin.elements.impl.fire;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.BaseElement;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.Ability;
import saturn.elementPlugin.elements.abilities.impl.fire.HellishFlamesAbility;
import saturn.elementPlugin.elements.abilities.impl.fire.PhoenixFormAbility;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class FireElement extends BaseElement {
    private final Ability ability1;
    private final PhoenixFormAbility ability2; // Changed type to access clearCooldown()

    public FireElement(ElementPlugin plugin) {
        super(plugin);
        this.ability1 = new HellishFlamesAbility(plugin);
        this.ability2 = new PhoenixFormAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.FIRE;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Fire Resistance (always active)
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, true, false));

        // Upside 2: Fire Aspect on hits (requires Upgrade 2, handled in FireCombatListener)
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
        player.removePotionEffect(PotionEffectType.FIRE_RESISTANCE);

        // Clear Phoenix Form metadata
        player.removeMetadata(PhoenixFormAbility.META_PHOENIX_INVULNERABLE, plugin);

        // CRITICAL FIX: Clear Phoenix Form cooldown
        ability2.clearCooldown(player);

        // Clear Hellish Flames metadata
        player.removeMetadata(HellishFlamesAbility.META_HELLISH_FLAMES, plugin);

        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.RED + "Fire";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Masters of flame and destruction. Fire users are immune to fire damage and can rain destruction from above.";
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