package hs.elementPlugin.elements.life;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.abilities.Ability;
import hs.elementPlugin.elements.abilities.life.LifeHealingBeamAbility;
import hs.elementPlugin.elements.abilities.life.LifeRegenAbility;
import hs.elementPlugin.elements.BaseElement;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class LifeElement extends BaseElement {
    private final ElementPlugin plugin;
    private final Ability ability1;
    private final Ability ability2;

    public LifeElement(ElementPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
        this.ability1 = new LifeRegenAbility(plugin);
        this.ability2 = new LifeHealingBeamAbility(plugin);
    }

    @Override
    public ElementType getType() { return ElementType.LIFE; }

    @Override
    public void applyUpsides(Player player, int upgradeLevel) {
        // Upside 1: 15 hearts (30 HP)
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            if (attr.getBaseValue() < 30.0) attr.setBaseValue(30.0);
            if (player.getHealth() > attr.getBaseValue()) player.setHealth(attr.getBaseValue());
        }
        
        // Upside 2: Crops within 5x5 radius instantly grow (passive effect)
        if (upgradeLevel >= 2) {
            // This is a passive effect that triggers automatically
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }
                    
                    // Grow crops in 5x5 around player every 5 seconds
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            Block b = player.getLocation().add(dx, 0, dz).getBlock();
                            growIfCrop(b);
                            growIfCrop(b.getRelative(0, 1, 0));
                        }
                    }
                }
            }.runTaskTimer(plugin, 0L, 100L); // Every 5 seconds
        }
    }

    private void growIfCrop(Block block) {
        if (block.getBlockData() instanceof Ageable ageable) {
            if (ageable.getAge() < ageable.getMaximumAge()) {
                ageable.setAge(ageable.getMaximumAge());
                block.setBlockData(ageable);
            }
        }
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
        // Reset max health to default
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) player.setHealth(20.0);
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
        return "Masters of healing and growth. Life users have increased health and can heal allies.";
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