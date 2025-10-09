package hs.elementPlugin.elements.impl.air;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.impl.air.AirBlastAbility;
import hs.elementPlugin.elements.abilities.impl.air.AirDashAbility;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class AirElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    public AirElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new AirBlastAbility(plugin);
        this.ability2 = new AirDashAbility(plugin);
    }

    @Override
    public ElementType getType() {
        return ElementType.AIR;
    }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, true, false));
    }

    @Override
    protected boolean executeAbility1(ElementContext context) {
        Player player = context.getPlayer();

        if (!context.getManaManager().hasMana(player, ability1.getManaCost())) {
            player.sendMessage(ChatColor.RED + "Not enough mana! (" + ability1.getManaCost() + " required)");
            return false;
        }

        context.getManaManager().spend(player, ability1.getManaCost());
        return ability1.execute(context);
    }

    @Override
    protected boolean executeAbility2(ElementContext context) {
        Player player = context.getPlayer();
        
        if (!context.getManaManager().hasMana(player, ability2.getManaCost())) {
            player.sendMessage(ChatColor.RED + "Not enough mana! (" + ability2.getManaCost() + " required)");
            return false;
        }

        context.getManaManager().spend(player, ability2.getManaCost());
        return ability2.execute(context);
    }
    
    @Override
    public void clearEffects(Player player) {
        ability1.setActive(player, false);
        ability2.setActive(player, false);
    }
    
    @Override
    public String getDisplayName() {
        return ChatColor.WHITE + "Air";
    }
    
    @Override
    public String getDescription() {
        return "Master the swift and agile power of air to control movement and push enemies.";
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