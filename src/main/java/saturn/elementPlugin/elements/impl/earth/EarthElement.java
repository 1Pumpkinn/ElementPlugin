package saturn.elementPlugin.elements.impl.earth;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.BaseElement;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.Ability;
import saturn.elementPlugin.elements.abilities.impl.earth.EarthTunnelAbility;
import saturn.elementPlugin.elements.abilities.impl.earth.EarthquakeAbility;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EarthElement extends BaseElement {
    public static final String META_MINE_UNTIL = "earth_mine_until";
    public static final String META_CHARM_NEXT_UNTIL = "earth_charm_next_until";
    public static final String META_TUNNELING = "earth_tunneling";

    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    public EarthElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new EarthTunnelAbility(plugin);
        this.ability2 = new EarthquakeAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.EARTH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, Integer.MAX_VALUE, 0, true, false));
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
    protected boolean canCancelAbility1(ElementContext context) {
        // Check if the player has the tunneling metadata - if so, they can cancel
        boolean canCancel = context.getPlayer().hasMetadata(META_TUNNELING);
        if (canCancel) {

        }
        return canCancel;
    }

    @Override
    public void clearEffects(Player player) {
        player.removePotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE);
        player.removeMetadata(META_MINE_UNTIL, plugin);
        player.removeMetadata(META_CHARM_NEXT_UNTIL, plugin);
        player.removeMetadata(META_TUNNELING, plugin);
        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.YELLOW + "Earth";
    }

    @Override
    public String getDescription() {
        return "Masters of stone and earth. Earth users can tunnel through blocks and charm mobs.";
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