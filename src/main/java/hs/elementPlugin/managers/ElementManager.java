package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.*;
import hs.elementPlugin.elements.impl.air.AirElement;
import hs.elementPlugin.elements.impl.water.WaterElement;
import hs.elementPlugin.elements.impl.fire.FireElement;
import hs.elementPlugin.elements.impl.earth.EarthElement;
import hs.elementPlugin.elements.impl.life.LifeElement;
import hs.elementPlugin.elements.impl.death.DeathElement;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class ElementManager {

    private static final ElementType[] BASIC_ELEMENTS = {
            ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH
    };

    private static final int ROLL_STEPS = 16;
    private static final long ROLL_DELAY_TICKS = 3L;

    private final ElementPlugin plugin;
    private final DataStore store;
    private final ManaManager manaManager;
    private final TrustManager trustManager;
    private final ConfigManager configManager;
    private final Map<ElementType, Element> registry = new EnumMap<>(ElementType.class);
    private final Set<UUID> currentlyRolling = new HashSet<>();
    private final Random random = new Random();

    public ElementManager(ElementPlugin plugin, DataStore store, ManaManager manaManager,
                          TrustManager trustManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.store = store;
        this.manaManager = manaManager;
        this.trustManager = trustManager;
        this.configManager = configManager;
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

    public void cancelRolling(Player player) {
        currentlyRolling.remove(player.getUniqueId());
    }

    public void rollAndAssign(Player player) {
        if (!beginRoll(player)) return;

        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);

        new RollingAnimation(player, BASIC_ELEMENTS)
                .withSteps(ROLL_STEPS)
                .withDelay(ROLL_DELAY_TICKS)
                .onComplete(() -> {
                    assignRandomWithTitle(player);
                    endRoll(player);
                })
                .start();
    }

    private void assignRandomWithTitle(Player player) {
        ElementType randomType = BASIC_ELEMENTS[random.nextInt(BASIC_ELEMENTS.length)];
        assignRandomWithTitle(player, randomType);
    }

    private void assignRandomWithTitle(Player player, ElementType targetElement) {
        PlayerData pd = data(player.getUniqueId());
        ElementType oldElement = pd.getCurrentElement();

        if (oldElement != null && oldElement != targetElement) {
            clearOldElementEffects(player, oldElement);
            returnLifeOrDeathCore(player, oldElement);
        }

        int currentUpgradeLevel = pd.getCurrentElementUpgradeLevel();
        pd.setCurrentElementWithoutReset(targetElement);
        pd.setCurrentElementUpgradeLevel(currentUpgradeLevel);
        store.save(pd);

        showElementTitle(player, targetElement, "Element Assigned!");
        applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public void assignRandomDifferentElement(Player player) {
        ElementType current = getPlayerElement(player);

        List<ElementType> available = Arrays.stream(BASIC_ELEMENTS)
                .filter(type -> type != current)
                .toList();

        ElementType newType = available.isEmpty() ?
                BASIC_ELEMENTS[random.nextInt(BASIC_ELEMENTS.length)] :
                available.get(random.nextInt(available.size()));

        assignElementInternal(player, newType, "Element Rerolled!");
    }

    public void assignElement(Player player, ElementType type) {
        assignElementInternal(player, type, "Element Chosen!", true);
    }

    public void setElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        if (old != null && old != type) {
            returnLifeOrDeathCore(player, old);
            clearOldElementEffects(player, old);
        }

        pd.setCurrentElement(type);
        store.save(pd);

        player.sendMessage(ChatColor.GOLD + "Your element is now " + ChatColor.AQUA + type.name());
        applyUpsides(player);
    }

    private void assignElementInternal(Player player, ElementType type, String titleText) {
        assignElementInternal(player, type, titleText, false);
    }

    private void assignElementInternal(Player player, ElementType type, String titleText, boolean resetLevel) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        if (old != type) {
            returnLifeOrDeathCore(player, old);
        }

        if (old != null && old != type) {
            clearOldElementEffects(player, old);
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
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    private void clearOldElementEffects(Player player, ElementType oldElement) {
        if (oldElement == null) return;

        Element element = registry.get(oldElement);
        if (element != null) {
            element.clearEffects(player);
        }

        if (oldElement == ElementType.LIFE) {
            Optional.ofNullable(player.getAttribute(Attribute.MAX_HEALTH))
                    .ifPresent(attr -> {
                        attr.setBaseValue(20.0);
                        if (!player.isDead() && player.getHealth() > attr.getBaseValue()) {
                            player.setHealth(attr.getBaseValue());
                        }
                    });
        }
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
                .configManager(configManager)
                .plugin(plugin)
                .build();

        return number == 1 ? element.ability1(ctx) : element.ability2(ctx);
    }

    public void giveElementItem(Player player, ElementType type) {
        Optional.ofNullable(hs.elementPlugin.items.ElementCoreItem.createCore(plugin, type))
                .ifPresent(item -> {
                    player.getInventory().addItem(item);
                    player.sendMessage(ChatColor.GREEN + "You received a " +
                            hs.elementPlugin.items.ElementCoreItem.getDisplayName(type) + " item!");
                });
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

    private void returnLifeOrDeathCore(Player player, ElementType oldElement) {
        if (oldElement != ElementType.LIFE && oldElement != ElementType.DEATH) return;
        if (!data(player.getUniqueId()).hasElementItem(oldElement)) return;

        Optional.ofNullable(hs.elementPlugin.items.ElementCoreItem.createCore(plugin, oldElement))
                .ifPresent(core -> {
                    player.getInventory().addItem(core);
                    player.sendMessage(ChatColor.YELLOW + "Your " +
                            hs.elementPlugin.items.ElementCoreItem.getDisplayName(oldElement) +
                            ChatColor.YELLOW + " has been returned to you!");
                });
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

    private class RollingAnimation {
        private final Player player;
        private final ElementType[] elements;
        private int steps = 16;
        private long delayTicks = 3L;
        private Runnable onComplete;

        RollingAnimation(Player player, ElementType[] elements) {
            this.player = player;
            this.elements = elements;
        }

        RollingAnimation withSteps(int steps) {
            this.steps = steps;
            return this;
        }

        RollingAnimation withDelay(long ticks) {
            this.delayTicks = ticks;
            return this;
        }

        RollingAnimation onComplete(Runnable callback) {
            this.onComplete = callback;
            return this;
        }

        void start() {
            new BukkitRunnable() {
                int tick = 0;

                @Override
                public void run() {
                    if (!player.isOnline() || !isCurrentlyRolling(player)) {
                        endRoll(player);
                        cancel();
                        return;
                    }

                    if (tick >= steps) {
                        if (onComplete != null) onComplete.run();
                        cancel();
                        return;
                    }

                    String randomName = elements[random.nextInt(elements.length)].name();
                    player.sendTitle(ChatColor.GOLD + "Rolling...", ChatColor.AQUA + randomName, 0, 10, 0);
                    tick++;
                }
            }.runTaskTimer(plugin, 0L, delayTicks);
        }
    }
}