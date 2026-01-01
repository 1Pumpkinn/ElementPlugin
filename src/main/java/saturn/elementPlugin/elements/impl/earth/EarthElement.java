package saturn.elementPlugin.elements.impl.earth;

import org.bukkit.Sound;
import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.BaseElement;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.Ability;
import saturn.elementPlugin.elements.abilities.impl.earth.EarthquakeAbility;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EarthElement extends BaseElement {
    public static final String META_CHARM_NEXT_UNTIL = "earth_charm_next_until";

    private final ElementPlugin plugin;
    private final Ability ability2;

    public EarthElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability2 = new EarthquakeAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.EARTH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Golden apples give 1 more heart of absorption (handled in EarthGoldenAppleListener)
        // Upside 2: 1.5x ore drops (requires Upgrade II, handled in EarthOreDropListener)
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();

        // Apply Haste 5 for 10 seconds
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 200, 4, true, true, true));
        player.sendMessage(ChatColor.GOLD + "Mining Frenzy activated for 10 seconds!");
        player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 0.8f);

        return true;
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        return ability2.execute(context);
    }

    @Override
    protected boolean canCancelAbility1(ElementContext context) {
        return false; // No longer cancellable since it's just a buff
    }

    @Override
    public void clearEffects(Player player) {
        player.removeMetadata(META_CHARM_NEXT_UNTIL, plugin);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.YELLOW + "Earth";
    }

    @Override
    public String getDescription() {
        return "Masters of stone and earth. Earth users have enhanced mining capabilities.";
    }

    @Override
    public String getAbility1Name() {
        return ChatColor.GOLD + "Mining Frenzy";
    }

    @Override
    public String getAbility1Description() {
        return "Gain Haste 5 for 10 seconds, allowing extremely fast mining.";
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