package saturn.elementPlugin.elements.impl.death;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.elements.BaseElement;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.abilities.Ability;
import saturn.elementPlugin.elements.abilities.impl.death.DeathClockAbility;
import saturn.elementPlugin.elements.abilities.impl.death.DeathSlashAbility;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class DeathElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    // Metadata key for tracking invisibility activation
    public static final String META_DEATH_INVIS_COOLDOWN = "death_invis_cooldown";

    public DeathElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new DeathSlashAbility(plugin);
        this.ability2 = new DeathClockAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.DEATH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: 25% more XP from kills (handled in DeathXPDropListener)
        // No potion effect needed here

        // Upside 2: Go invisible for 10 seconds when on 2 hearts (requires Upgrade II)
        // Handled by DeathInvisibilityListener
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
        // Clear ability metadata
        player.removeMetadata(DeathClockAbility.META_DEATH_CLOCK_ACTIVE, plugin);
        player.removeMetadata(DeathSlashAbility.META_SLASH_ACTIVE, plugin);
        player.removeMetadata(DeathSlashAbility.META_BLEEDING, plugin);
        player.removeMetadata(META_DEATH_INVIS_COOLDOWN, plugin);

        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.DARK_PURPLE + "Death";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Master of curses and afflictions. Death users can curse enemies and cause bleeding.";
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