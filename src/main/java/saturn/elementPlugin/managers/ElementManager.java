package saturn.elementPlugin.managers;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.DataStore;
import saturn.elementPlugin.data.PlayerData;
import saturn.elementPlugin.elements.Element;
import saturn.elementPlugin.elements.ElementContext;
import saturn.elementPlugin.elements.ElementType;
import saturn.elementPlugin.elements.impl.air.AirElement;
import saturn.elementPlugin.elements.impl.death.DeathElement;
import saturn.elementPlugin.elements.impl.earth.EarthElement;
import saturn.elementPlugin.elements.impl.fire.FireElement;
import saturn.elementPlugin.elements.impl.life.LifeElement;
import saturn.elementPlugin.elements.impl.water.WaterElement;
import saturn.elementPlugin.util.SmartEffectCleaner;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

/**
 * Manages element assignment, abilities, and passive effects
 * Reroller logic has been moved to reroller listeners
 */
public class ElementManager {

    private final ElementPlugin plugin;
    private final DataStore store;
    private final ManaManager manaManager;
    private final TeamManager teamManager;
    private final Map<ElementType, Element> registry = new EnumMap<>(ElementType.class);
    private final Set<UUID> currentlyRolling = new HashSet<>();

    public ElementManager(ElementPlugin plugin, DataStore store, ManaManager manaManager,
                          TeamManager teamManager) {
        this.plugin = plugin;
        this.store = store;
        this.manaManager = manaManager;
        this.teamManager = teamManager;
        registerAllElements();
    }

    public ElementPlugin getPlugin() { return plugin; }

    private void registerAllElements() {
        registerElement(ElementType.AIR, () -> new AirElement(plugin));
        registerElement(ElementType.WATER, () -> new WaterElement(plugin));
        registerElement(ElementType.FIRE, () -> new FireElement(plugin));
        registerElement(ElementType.EARTH, () -> new EarthElement(plugin));
        registerElement(ElementType.LIFE, () -> new LifeElement(plugin));
        registerElement(ElementType.DEATH, () -> new DeathElement(plugin));
        registerElement(ElementType.METAL, () -> new saturn.elementPlugin.elements.impl.metal.MetalElement(plugin));
        registerElement(ElementType.FROST, () -> new saturn.elementPlugin.elements.impl.frost.FrostElement(plugin));
    }

    private void registerElement(ElementType type, Supplier<Element> supplier) {
        registry.put(type, supplier.get());
    }

    public PlayerData data(@NotNull UUID uuid) {
        return store.getPlayerData(uuid);
    }

    public Element get(ElementType type) {
        return registry.get(type);
    }

    public ElementType getPlayerElement(Player player) {
        return Optional.ofNullable(data(player.getUniqueId()))
                .map(PlayerData::getElementType)
                .orElse(null);
    }


    // ROLLING STATE MANAGEMENT (for rerollers)

    /**
     * Check if player is currently rolling for an element
     * Used by reroller listeners to prevent double-usage
     */
    public boolean isCurrentlyRolling(Player player) {
        return currentlyRolling.contains(player.getUniqueId());
    }

    /**
     * Set player's rolling state
     * Called by reroller listeners during animation
     */
    public void setCurrentlyRolling(Player player, boolean rolling) {
        if (rolling) {
            currentlyRolling.add(player.getUniqueId());
        } else {
            currentlyRolling.remove(player.getUniqueId());
        }
    }

    /**
     * Cancel rolling for a player
     * Called when player disconnects during roll or animation completes
     */
    public void cancelRolling(Player player) {
        currentlyRolling.remove(player.getUniqueId());
    }

    // ELEMENT ASSIGNMENT (DIRECT)

     /**Assign a random basic element to a new player
        No animation - just picks and assigns
        Called on first join*/
    public void rollAndAssign(Player player) {
        ElementType[] basicElements = {
                ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH
        };

        Random random = new Random();
        ElementType randomType = basicElements[random.nextInt(basicElements.length)];

        assignElement(player, randomType);

        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        player.sendMessage(ChatColor.GREEN + "You have been assigned " +
                ChatColor.AQUA + randomType.name() + ChatColor.GREEN + "!");
    }

    /** Directly assign an element to a player with title
        Used by admin commands and first join */
    public void assignElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        // Clear old element effects if changing
        if (old != null && old != type) {
            SmartEffectCleaner.clearForElementChange(plugin, player);
        }

        // Set new element (resets upgrade level)
        pd.setCurrentElement(type);
        store.save(pd);

        showElementTitle(player, type, "Element Chosen!");
        // Apply new element effects
        applyUpsides(player);
    }

    /** Set element without resetting upgrade level
        Used by admin commands */
    public void setElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        // Clear old element effects if changing
        if (old != null && old != type) {
            SmartEffectCleaner.clearForElementChange(plugin, player);
        }

        // Preserve upgrade level
        int currentUpgrade = pd.getCurrentElementUpgradeLevel();
        pd.setCurrentElementWithoutReset(type);
        pd.setCurrentElementUpgradeLevel(currentUpgrade);
        store.save(pd);

        player.sendMessage(ChatColor.GOLD + "Your element is now " + ChatColor.AQUA + type.name());
        applyUpsides(player);
    }


     // Show element title to player
    private void showElementTitle(Player player, ElementType type, String title) {
        var titleObj = net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text(title)
                        .color(net.kyori.adventure.text.format.NamedTextColor.GOLD),
                net.kyori.adventure.text.Component.text(type.name())
                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(2000),
                        java.time.Duration.ofMillis(500)
                ));
        player.showTitle(titleObj);
    }

    // PASSIVE EFFECTS
    public void applyUpsides(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();

        if (type == null) return;

        Element element = registry.get(type);
        if (element != null) {
            element.applyUpsides(player, pd.getUpgradeLevel(type));
        }
    }


    // ABILITY EXECUTION
    public boolean useAbility1(Player player) {
        return useAbility(player, 1);
    }

    public boolean useAbility2(Player player) {
        return useAbility(player, 2);
    }

    private boolean useAbility(Player player, int number) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        Element element = registry.get(type);

        if (element == null) return false;

        ElementContext ctx = ElementContext.builder()
                .player(player)
                .upgradeLevel(pd.getUpgradeLevel(type))
                .elementType(type)
                .manaManager(manaManager)
                .trustManager(teamManager)
                .plugin(plugin)
                .build();

        return number == 1 ? element.ability1(ctx) : element.ability2(ctx);
    }
}