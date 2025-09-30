package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.impl.AirElement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class ElementManager {
    private final ElementPlugin plugin;
    private final DataStore store;
    private final ManaManager manaManager;
    private final TrustManager trustManager;
    private final CooldownManager cooldownManager;
    private final ConfigManager configManager;

    private final Map<ElementType, Element> registry = new EnumMap<>(ElementType.class);
    private final Random random = new Random();

    public ElementManager(ElementPlugin plugin, DataStore store, ManaManager manaManager, TrustManager trustManager, CooldownManager cooldownManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.store = store;
        this.manaManager = manaManager;
        this.trustManager = trustManager;
        this.cooldownManager = cooldownManager;
        this.configManager = configManager;

        // Register elements
        register(new AirElement());
        register(new hs.elementPlugin.elements.impl.WaterElement());
        register(new hs.elementPlugin.elements.impl.FireElement());
        register(new hs.elementPlugin.elements.impl.EarthElement());
        register(new hs.elementPlugin.elements.impl.LifeElement());
    }

    private void register(Element element) {
        registry.put(element.getType(), element);
    }

    public PlayerData data(UUID uuid) { return manaManager.get(uuid); }

    public Element get(ElementType type) { return registry.get(type); }

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

    public void rollAndAssign(Player player) {
        // Slowing title roll animation, then assign
        String[] names = {"AIR", "WATER", "FIRE", "EARTH"};
        player.playSound(player.getLocation(), Sound.UI_TOAST_IN, 1f, 1.2f);
        int steps = 16; // number of flashes
        for (int i = 0; i < steps; i++) {
            int delay = i * 3; // slows slightly over time
            int idx = i % names.length;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                String name = names[random.nextInt(names.length)];
                player.sendTitle(ChatColor.YELLOW + "Rolling...", ChatColor.AQUA + name, 0, 10, 0);
            }, delay);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> assignRandomWithTitle(player), steps * 3L + 2L);
    }

    public void assignRandomWithTitle(Player player) {
        // Randomly assign one of the 4 basic elements
        ElementType[] choices = new ElementType[]{ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH};
        ElementType pick = choices[random.nextInt(choices.length)];
        PlayerData pd = data(player.getUniqueId());
        pd.setCurrentElement(pick);
        store.save(pd);
        player.sendTitle(ChatColor.GOLD + "Attuned!", ChatColor.AQUA + pick.name(), 10, 40, 10);
        applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public void setElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());
        pd.setCurrentElement(type);
        store.save(pd);
        player.sendMessage(ChatColor.GOLD + "Your element is now " + ChatColor.AQUA + type.name());
        applyUpsides(player);
    }

    public boolean useAbility1(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        if (type == null) return false;
        Element e = registry.get(type);
        if (e == null) return false;
        return e.ability1(player, pd.getUpgradeLevel(type), manaManager, cooldownManager, trustManager, configManager);
    }

    public boolean useAbility2(Player player) {
        PlayerData pd = data(player.getUniqueId());
        ElementType type = pd.getCurrentElement();
        if (type == null) return false;
        Element e = registry.get(type);
        if (e == null) return false;
        return e.ability2(player, pd.getUpgradeLevel(type), manaManager, cooldownManager, trustManager, configManager);
    }
}