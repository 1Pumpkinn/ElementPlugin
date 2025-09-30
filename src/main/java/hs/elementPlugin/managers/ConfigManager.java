package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {
    private final ElementPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // Mana settings
    public int getMaxMana() {
        return config.getInt("mana.max", 100);
    }

    public int getManaRegenPerSecond() {
        return config.getInt("mana.regen_per_second", 1);
    }

    // Ability costs
    public int getAbility1Cost(ElementType type) {
        String path = "costs." + type.name().toLowerCase() + ".ability1";
        return config.getInt(path, 50);
    }

    public int getAbility2Cost(ElementType type) {
        String path = "costs." + type.name().toLowerCase() + ".ability2";
        return config.getInt(path, 75);
    }

    public int getItemUseCost(ElementType type) {
        String path = "costs." + type.name().toLowerCase() + ".item_use";
        return config.getInt(path, 75);
    }

    public int getItemThrowCost(ElementType type) {
        String path = "costs." + type.name().toLowerCase() + ".item_throw";
        return config.getInt(path, 25);
    }

    // Cooldowns (in seconds)
    public int getCooldown(ElementType type, String ability) {
        String path = "cooldowns." + type.name().toLowerCase() + "." + ability;
        return config.getInt(path, 0);
    }
}