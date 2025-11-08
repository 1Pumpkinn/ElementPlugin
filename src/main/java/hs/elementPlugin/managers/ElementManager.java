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
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ElementManager {
    private final ElementPlugin plugin;
    private final DataStore store;
    private final ManaManager manaManager;
    private final TrustManager trustManager;
    private final ConfigManager configManager;

    private final Map<ElementType, Element> registry = new EnumMap<>(ElementType.class);
    private final Set<UUID> currentlyRolling = new HashSet<>();
    private final Random random = new Random();

    private static final ElementType[] BASIC_ELEMENTS = {
            ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH
    };

    public ElementManager(ElementPlugin plugin, DataStore store,
                          ManaManager manaManager, TrustManager trustManager,
                          ConfigManager configManager) {
        this.plugin = plugin;
        this.store = store;
        this.manaManager = manaManager;
        this.trustManager = trustManager;
        this.configManager = configManager;

        // Register all elements
        register(new AirElement(plugin));
        register(new WaterElement(plugin));
        register(new FireElement(plugin));
        register(new EarthElement(plugin));
        register(new LifeElement(plugin));
        register(new DeathElement(plugin));
        register(new hs.elementPlugin.elements.impl.metal.MetalElement(plugin));
        register(new hs.elementPlugin.elements.impl.frost.FrostElement(plugin));
    }

    private void register(Element element) {
        registry.put(element.getType(), element);
    }

    public PlayerData data(@NotNull UUID uuid) {
        return store.getPlayerData(uuid);
    }

    public Element get(ElementType type) {
        return registry.get(type);
    }

    public ElementType getPlayerElement(Player player) {
        PlayerData pd = data(player.getUniqueId());
        return pd != null ? pd.getElementType() : null;
    }

    public void ensureAssigned(Player player) {
        PlayerData pd = data(player.getUniqueId());
        if (pd.getCurrentElement() == null) rollAndAssign(player);
    }

    public boolean isCurrentlyRolling(Player player) {
        return currentlyRolling.contains(player.getUniqueId());
    }

    /* ------------------------------  ELEMENT ASSIGNMENT ------------------------------ */

    public void rollAndAssign(Player player) {
        if (!beginRoll(player)) return;

        String[] rollingNames = Arrays.stream(BASIC_ELEMENTS).map(Enum::name).toArray(String[]::new);

        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);
        int steps = 16;
        long delayPerStep = 3L;

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (tick >= steps) {
                    assignRandomWithTitle(player);
                    endRoll(player);
                    cancel();
                    return;
                }
                String randomName = rollingNames[random.nextInt(rollingNames.length)];
                player.sendTitle(ChatColor.GOLD + "Rolling...", ChatColor.AQUA + randomName, 0, 10, 0);
                tick++;
            }
        }.runTaskTimer(plugin, 0L, delayPerStep);
    }

    public void assignRandomWithTitle(Player player) {
        assignElementInternal(player, randomChoice(BASIC_ELEMENTS), "Element Chosen!");
    }

    public void assignRandomDifferentElement(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType current = pd.getCurrentElement();

        ElementType[] choices = BASIC_ELEMENTS;
        if (!Arrays.asList(BASIC_ELEMENTS).contains(current)) {
            assignRandomWithTitle(player);
            return;
        }

        List<ElementType> available = new ArrayList<>(Arrays.asList(BASIC_ELEMENTS));
        available.remove(current);

        assignElementInternal(player, randomChoice(available), "Element Rerolled!");
    }

    public void assignElement(Player player, ElementType type) {
        assignElementInternal(player, type, "Element Chosen!", true);
    }

    public void setElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        if (old != type) returnLifeOrDeathCore(player, old);
        clearAllEffects(player);

        pd.setCurrentElement(type);
        store.save(pd);

        plugin.getLogger().info("[ElementManager] Set element for " + player.getName() + " to " + type);
        player.sendMessage(ChatColor.GOLD + "Your element is now " + ChatColor.AQUA + type.name());

        applyUpsides(player);
    }

    private void assignElementInternal(Player player, ElementType type, String titleText) {
        assignElementInternal(player, type, titleText, false);
    }

    private void assignElementInternal(Player player, ElementType type, String titleText, boolean resetLevel) {
        PlayerData pd = data(player.getUniqueId());
        ElementType old = pd.getCurrentElement();

        if (old != type) returnLifeOrDeathCore(player, old);
        clearAllEffects(player);

        int currentUpgrade = pd.getCurrentElementUpgradeLevel();
        if (resetLevel) pd.setCurrentElement(type);
        else {
            pd.setCurrentElementWithoutReset(type);
            pd.setCurrentElementUpgradeLevel(currentUpgrade);
        }

        store.save(pd);
        showElementTitle(player, type, titleText);
        applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    /* ------------------------------  ELEMENT VISUALS ------------------------------ */

    private void showElementTitle(Player player, ElementType type, String title) {
        var titleObj = net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text(title).color(net.kyori.adventure.text.format.NamedTextColor.GOLD),
                net.kyori.adventure.text.Component.text(type.name())
                        .color(net.kyori.adventure.text.format.NamedTextColor.AQUA),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(500),
                        java.time.Duration.ofMillis(2000),
                        java.time.Duration.ofMillis(500)
                )
        );
        player.showTitle(titleObj);
    }

    private void clearAllEffects(Player player) {
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        var attr = player.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            player.setHealth(Math.min(player.getHealth(), 20.0));
        }
    }

    public void applyUpsides(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        if (type == null) return;
        Element element = registry.get(type);
        if (element != null) element.applyUpsides(player, pd.getUpgradeLevel(type));
    }

    /* ------------------------------  ROLL TRACKING ------------------------------ */

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

    /* ------------------------------  LIFE/DEATH CORE ------------------------------ */

    private void returnLifeOrDeathCore(Player player, ElementType oldElement) {
        if (oldElement == null || (oldElement != ElementType.LIFE && oldElement != ElementType.DEATH)) return;

        PlayerData pd = data(player.getUniqueId());
        if (!pd.hasElementItem(oldElement)) return;

        ItemStack core = hs.elementPlugin.items.ElementCoreItem.createCore(plugin, oldElement);
        if (core != null) {
            player.getInventory().addItem(core);
            player.sendMessage(ChatColor.YELLOW + "Your " +
                    hs.elementPlugin.items.ElementCoreItem.getDisplayName(oldElement) +
                    ChatColor.YELLOW + " has been returned to you!");
        }
    }

    /* ------------------------------  ABILITIES ------------------------------ */

    public boolean useAbility1(Player player) {
        return useAbility(player, 1);
    }

    public boolean useAbility2(Player player) {
        return useAbility(player, 2);
    }

    private boolean useAbility(Player player, int ability) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        Element element = registry.get(type);
        if (element == null) return false;

        ElementContext ctx = new ElementContext(
                player, pd.getUpgradeLevel(type), type,
                manaManager, trustManager, configManager, plugin
        );
        return ability == 1 ? element.ability1(ctx) : element.ability2(ctx);
    }

    /* ------------------------------  ITEMS ------------------------------ */

    public void giveElementItem(Player player, ElementType type) {
        ItemStack item = hs.elementPlugin.items.ElementCoreItem.createCore(plugin, type);
        if (item != null) {
            player.getInventory().addItem(item);
            player.sendMessage(ChatColor.GREEN + "You received a " +
                    hs.elementPlugin.items.ElementCoreItem.getDisplayName(type) + " item!");
        }
    }

    /* ------------------------------  UTILITY ------------------------------ */

    private <T> T randomChoice(T[] array) {
        return array[random.nextInt(array.length)];
    }

    private <T> T randomChoice(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }
}