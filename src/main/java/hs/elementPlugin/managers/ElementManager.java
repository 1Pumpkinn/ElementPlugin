package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.impl.air.AirElement;
import hs.elementPlugin.elements.impl.death.DeathElement;
import hs.elementPlugin.elements.impl.earth.EarthElement;
import hs.elementPlugin.elements.impl.fire.FireElement;
import hs.elementPlugin.elements.impl.life.LifeElement;
import hs.elementPlugin.elements.impl.water.WaterElement;
import hs.elementPlugin.util.SmartEffectCleaner;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class ElementManager {

    private final ElementPlugin plugin;
    private final DataStore store;
    private final ManaManager manaManager;
    private final TrustManager trustManager;
    private final Map<ElementType, Element> registry = new EnumMap<>(ElementType.class);
    private final Set<UUID> currentlyRolling = new HashSet<>();

    public ElementManager(ElementPlugin plugin, DataStore store, ManaManager manaManager,
                          TrustManager trustManager) {
        this.plugin = plugin;
        this.store = store;
        this.manaManager = manaManager;
        this.trustManager = trustManager;
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
        registerElement(ElementType.METAL, () -> new hs.elementPlugin.elements.impl.metal.MetalElement(plugin));
        registerElement(ElementType.FROST, () -> new hs.elementPlugin.elements.impl.frost.FrostElement(plugin));
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

    public void ensureAssigned(Player player) {
        if (getPlayerElement(player) == null) {
            rollAndAssign(player);
        }
    }

    public boolean isCurrentlyRolling(Player player) {
        return currentlyRolling.contains(player.getUniqueId());
    }

    public void setCurrentlyRolling(Player player, boolean rolling) {
        if (rolling) {
            currentlyRolling.add(player.getUniqueId());
        } else {
            currentlyRolling.remove(player.getUniqueId());
        }
    }

    public void cancelRolling(Player player) {
        currentlyRolling.remove(player.getUniqueId());
    }

    /**
     * Roll and assign a random basic element (for first-time player assignment)
     * No animation - just picks and assigns
     */
    public void rollAndAssign(Player player) {
        ElementType[] basicElements = {
                ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH
        };

        Random random = new Random();
        ElementType randomType = basicElements[random.nextInt(basicElements.length)];
        assignElement(player, randomType);
    }

    public void assignElement(Player player, ElementType type) {
        assignElementInternal(player, type, "Element Chosen!", true);
    }

    /**
     * FIXED: Set element using SmartEffectCleaner
     */
    public void setElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        // FIXED: Use SmartEffectCleaner if changing elements
        if (old != null && old != type) {
            SmartEffectCleaner.clearForElementChange(plugin, player);
        }

        pd.setCurrentElement(type);
        store.save(pd);

        player.sendMessage(ChatColor.GOLD + "Your element is now " + ChatColor.AQUA + type.name());
        applyUpsides(player);
    }

    /**
     * Assign an element with title and optional level reset
     * @param player The player to assign element to
     * @param type The element type to assign
     * @param titleText The title text to display
     * @param resetLevel Whether to reset upgrade level (true) or preserve it (false)
     */
    public void assignElementWithTitle(Player player, ElementType type, String titleText, boolean resetLevel) {
        assignElementInternal(player, type, titleText, resetLevel);
    }

    private void assignElementInternal(Player player, ElementType type, String titleText, boolean resetLevel) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        // FIXED: Use SmartEffectCleaner if changing elements
        if (old != null && old != type) {
            SmartEffectCleaner.clearForElementChange(plugin, player);
        }

        if (resetLevel) {
            pd.setCurrentElement(type);
        } else {
            int currentUpgrade = pd.getCurrentElementUpgradeLevel();
            pd.setCurrentElementWithoutReset(type);
            pd.setCurrentElementUpgradeLevel(currentUpgrade);
        }
        store.save(pd);
        showElementTitle(player, type, titleText);
        applyUpsides(player);
    }

    public void applyUpsides(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();

        if (type == null) return;

        Element element = registry.get(type);
        if (element != null) {
            element.applyUpsides(player, pd.getUpgradeLevel(type));
        }
    }

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
                .trustManager(trustManager)
                .plugin(plugin)
                .build();

        return number == 1 ? element.ability1(ctx) : element.ability2(ctx);
    }

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

    private boolean beginRoll(Player player) {
        if (isCurrentlyRolling(player)) {
            player.sendMessage(ChatColor.RED + "You are already rerolling your element!");
            return false;
        }
        currentlyRolling.add(player.getUniqueId());
        return true;
    }

    private void endRoll(Player player) {
        currentlyRolling.remove(player.getUniqueId());
    }
}