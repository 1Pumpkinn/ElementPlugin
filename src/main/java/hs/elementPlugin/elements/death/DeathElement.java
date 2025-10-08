package hs.elementPlugin.elements.death;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.death.DeathWitherSkullAbility;
import hs.elementPlugin.elements.abilities.death.DeathSummonUndeadAbility;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Collection;

public class DeathElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    public DeathElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new DeathWitherSkullAbility(plugin);
        this.ability2 = new DeathSummonUndeadAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.DEATH;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: Any raw or undead foods act as golden apples (handled in a listener)
        // Upside 2: Nearby enemies get hunger 1 in a 5x5 radius (if upgradeLevel >= 2)
        if (upgradeLevel >= 2) {
            applyHungerToNearbyEnemies(player);
        }
    }

    private void applyHungerToNearbyEnemies(Player player) {
        Collection<LivingEntity> nearby = player.getLocation().getNearbyLivingEntities(5.0);
        for (LivingEntity entity : nearby) {
            if (entity instanceof Player target && !target.equals(player)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 60, 0, true, true, true)); // 3 seconds
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
        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }

    @Override
    public String getDisplayName() {
        return ChatColor.DARK_PURPLE + "Death";
    }

    @Override
    public String getDescription() {
        return ChatColor.GRAY + "Master of decay and the undead. Death users can corrupt food and summon wither powers.";
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
