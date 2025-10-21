package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.impl.air.AirElement;
import hs.elementPlugin.elements.impl.water.WaterElement;
import hs.elementPlugin.elements.impl.fire.FireElement;
import hs.elementPlugin.elements.impl.earth.EarthElement;
import hs.elementPlugin.elements.impl.life.LifeElement;
import hs.elementPlugin.elements.impl.death.DeathElement;
import hs.elementPlugin.items.ItemKeys;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ElementManager {
    private final ElementPlugin plugin;
    private final DataStore store;
    private final ManaManager manaManager;
    private final TrustManager trustManager;
    private final ConfigManager configManager;

    private final Map<ElementType, Element> registry = new EnumMap<>(ElementType.class);
    private final Random random = new Random();
    private final Set<UUID> currentlyRolling = new HashSet<>();

    public ElementManager(ElementPlugin plugin, DataStore store, ManaManager manaManager, TrustManager trustManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.store = store;
        this.manaManager = manaManager;
        this.trustManager = trustManager;
        this.configManager = configManager;

        // Register elements with plugin parameter only
        register(new AirElement(plugin));
        register(new WaterElement(plugin));
        register(new FireElement(plugin));
        register(new EarthElement(plugin));
        register(new LifeElement(plugin));
        register(new DeathElement(plugin));
    }

    public ElementType getPlayerElement(Player player) {
        PlayerData data = store.getPlayerData(player.getUniqueId());
        return data != null ? data.getElementType() : null;
    }

    private void register(Element element) {
        registry.put(element.getType(), element);
    }

    public PlayerData data(@NotNull UUID uuid) { return manaManager.get(uuid); }

    public Element get(ElementType type) { return registry.get(type); }

    private void clearAllEffects(Player player) {
        // Remove all potion effects
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        // Reset max health to default (20 HP)
        var attr = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(20.0);
            if (player.getHealth() > 20.0) player.setHealth(20.0);
        }
    }

    public void applyUpsides(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        if (type == null) return;
        Element e = registry.get(type);
        if (e != null) e.applyUpsides(player, pd.getUpgradeLevel(type));
    }

    public void ensureAssigned(Player player) {
        PlayerData pd = data(player.getUniqueId());
        if (pd.getCurrentElement() == null) {
            rollAndAssign(player);
        }
    }

    public boolean isCurrentlyRolling(Player player) {
        return currentlyRolling.contains(player.getUniqueId());
    }

    public void rollAndAssign(Player player) {
        // Check if player is already rolling
        if (isCurrentlyRolling(player)) {
            player.sendMessage(ChatColor.RED + "You are already rerolling your element!");
            return;
        }

        // Add player to currently rolling set
        currentlyRolling.add(player.getUniqueId());

        // Slowing title roll animation, then assign
        String[] names = {"AIR", "WATER", "FIRE", "EARTH"};
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);
        int steps = 16;
        for (int i = 0; i < steps; i++) {
            int delay = i * 3;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String name = names[random.nextInt(names.length)];
                player.sendTitle(ChatColor.YELLOW + "Rolling...", ChatColor.AQUA + name, 0, 10, 0);
            }, delay);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            assignRandomWithTitle(player);
            // Remove player from currently rolling set when done
            currentlyRolling.remove(player.getUniqueId());
        }, steps * 3L + 2L);
    }

    public void assignRandomWithTitle(Player player) {
        ElementType[] choices = new ElementType[]{ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH};
        ElementType pick = choices[random.nextInt(choices.length)];
        PlayerData pd = data(player.getUniqueId());

        // Return Life/Death core if player had one
        returnLifeOrDeathCore(player, pd.getCurrentElement());

        clearAllEffects(player);

        pd.setCurrentElement(pick);
        store.save(pd);
        player.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text("Element Chosen!").color(net.kyori.adventure.text.format.NamedTextColor.GOLD),
                net.kyori.adventure.text.Component.text(pick.name()).color(net.kyori.adventure.text.format.NamedTextColor.AQUA),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000), java.time.Duration.ofMillis(500))
        ));
        applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public void assignElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());

        // Return Life/Death core if player had one (and it's different from new element)
        ElementType oldElement = pd.getCurrentElement();
        if (oldElement != type) {
            returnLifeOrDeathCore(player, oldElement);
        }

        // Clear effects from previous element
        clearAllEffects(player);

        pd.setCurrentElement(type); // This automatically resets upgrade level
        store.save(pd);
        player.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.Component.text("Element Chosen!").color(net.kyori.adventure.text.format.NamedTextColor.GOLD),
                net.kyori.adventure.text.Component.text(type.name()).color(net.kyori.adventure.text.format.NamedTextColor.AQUA),
                net.kyori.adventure.title.Title.Times.times(java.time.Duration.ofMillis(500), java.time.Duration.ofMillis(2000), java.time.Duration.ofMillis(500))
        ));
        applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public void setElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());

        // Return Life/Death core if player had one (and it's different from new element)
        ElementType oldElement = pd.getCurrentElement();
        if (oldElement != type) {
            returnLifeOrDeathCore(player, oldElement);
        }

        // Clear effects from previous element
        clearAllEffects(player);

        pd.setCurrentElement(type);
        store.save(pd);
        player.sendMessage(
                net.kyori.adventure.text.Component.text("Your element is now ")
                        .color(net.kyori.adventure.text.format.NamedTextColor.GOLD)
                        .append(net.kyori.adventure.text.Component.text(type.name(), net.kyori.adventure.text.format.NamedTextColor.AQUA))
        );
        applyUpsides(player);
    }

    /**
     * Returns a Life or Death core to the player if they had one
     * @param player The player to return the core to
     * @param oldElement The element the player had before switching
     */
    private void returnLifeOrDeathCore(Player player, ElementType oldElement) {
        if (oldElement == null) return;

        // Only return if it was Life or Death
        if (oldElement != ElementType.LIFE && oldElement != ElementType.DEATH) return;

        PlayerData pd = data(player.getUniqueId());

        // Only return if they still have the element item flag
        // (if they died, this flag would have been removed)
        if (!pd.hasElementItem(oldElement)) return;

        // Create and give the core back
        ItemStack core = hs.elementPlugin.items.ElementCoreItem.createCore(plugin, oldElement);
        if (core != null) {
            player.getInventory().addItem(core);
            player.sendMessage(ChatColor.YELLOW + "Your " +
                    hs.elementPlugin.items.ElementCoreItem.getDisplayName(oldElement) +
                    ChatColor.YELLOW + " has been returned to you!");
        }
    }

    public boolean useAbility1(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        if (type == null) return false;
        Element e = registry.get(type);
        if (e == null) return false;

        ElementContext context = new ElementContext(
                player,
                pd.getUpgradeLevel(type),
                type,
                manaManager,
                trustManager,
                configManager,
                plugin
        );
        return e.ability1(context);
    }

    public boolean useAbility2(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        if (type == null) return false;
        Element e = registry.get(type);
        if (e == null) return false;

        ElementContext context = new ElementContext(
                player,
                pd.getUpgradeLevel(type),
                type,
                manaManager,
                trustManager,
                configManager,
                plugin
        );
        return e.ability2(context);
    }

    public void giveElementItem(Player player, ElementType elementType) {
        ItemStack item = hs.elementPlugin.items.ElementCoreItem.createCore(plugin, elementType);
        if (item != null) {
            player.getInventory().addItem(item);
            player.sendMessage(org.bukkit.ChatColor.GREEN + "You received a " + hs.elementPlugin.items.ElementCoreItem.getDisplayName(elementType) + " item!");
        }
    }
}