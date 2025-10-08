package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.data.DataStore;
import hs.elementPlugin.data.PlayerData;
import hs.elementPlugin.elements.Element;
import hs.elementPlugin.elements.ElementContext;
import hs.elementPlugin.elements.ElementType;
import hs.elementPlugin.elements.air.AirElement;
import hs.elementPlugin.elements.water.WaterElement;
import hs.elementPlugin.elements.fire.FireElement;
import hs.elementPlugin.elements.earth.EarthElement;
import hs.elementPlugin.elements.life.LifeElement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

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
    }
    
    public ElementType getPlayerElement(Player player) {
        PlayerData data = store.getPlayerData(player.getUniqueId());
        return data != null ? data.getElementType() : null;
    }

    private void register(Element element) {
        registry.put(element.getType(), element);
    }

    public PlayerData data(UUID uuid) { return manaManager.get(uuid); }

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
        // Randomly assign one of the 4 basic elements (no LIFE)
        ElementType[] choices = new ElementType[]{ElementType.AIR, ElementType.WATER, ElementType.FIRE, ElementType.EARTH};
        ElementType pick = choices[random.nextInt(choices.length)];
        PlayerData pd = data(player.getUniqueId());

        // Clear effects from previous element
        clearAllEffects(player);

        pd.setCurrentElement(pick); // This automatically resets upgrade level
        store.save(pd);
        player.sendTitle(ChatColor.GOLD + "Attuned!", ChatColor.AQUA + pick.name(), 10, 40, 10);
        applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }
    
    public void assignElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());

        // Clear effects from previous element
        clearAllEffects(player);

        pd.setCurrentElement(type); // This automatically resets upgrade level
        store.save(pd);
        player.sendTitle(ChatColor.GOLD + "Attuned!", ChatColor.AQUA + type.name(), 10, 40, 10);
        applyUpsides(player);
        player.getWorld().playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }
    
    public void setElement(Player player, ElementType type) {
        PlayerData pd = data(player.getUniqueId());

        // Clear effects from previous element
        clearAllEffects(player);

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
}