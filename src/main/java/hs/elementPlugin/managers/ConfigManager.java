package hs.elementPlugin.managers;

import hs.elementPlugin.ElementPlugin;
import hs.elementPlugin.elements.ElementType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Level;

public class ConfigManager {
    private final ElementPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(ElementPlugin plugin) {
        this.plugin = plugin;
        try {
            this.config = plugin.getConfig();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load plugin configuration", e);
            throw new RuntimeException("Could not load plugin configuration", e);
        }
    }

    public void reload() {
        try {
            plugin.reloadConfig();
            this.config = plugin.getConfig();
            plugin.getLogger().info("Configuration reloaded successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload configuration", e);
        }
    }

    // Mana settings
    public int getMaxMana() {
        try {
            return config.getInt("mana.max", 100);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error reading mana.max from config, using default value 100", e);
            return 100;
        }
    }

    public int getManaRegenPerSecond() {
        try {
            return config.getInt("mana.regen_per_second", 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error reading mana.regen_per_second from config, using default value 1", e);
            return 1;
        }
    }

    // Ability costs
    public int getAbility1Cost(ElementType type) {
        try {
            String path = "costs." + type.name().toLowerCase() + ".ability1";
            return config.getInt(path, 50);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error reading ability1 cost for " + type + " from config, using default value 50", e);
            return 50;
        }
    }

    public int getAbility2Cost(ElementType type) {
        try {
            String path = "costs." + type.name().toLowerCase() + ".ability2";
            return config.getInt(path, 75);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error reading ability2 cost for " + type + " from config, using default value 75", e);
            return 75;
        }
    }

    public int getItemUseCost(ElementType type) {
        try {
            String path = "costs." + type.name().toLowerCase() + ".item_use";
            return config.getInt(path, 75);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error reading item_use cost for " + type + " from config, using default value 75", e);
            return 75;
        }
    }

    public int getItemThrowCost(ElementType type) {
        try {
            String path = "costs." + type.name().toLowerCase() + ".item_throw";
            return config.getInt(path, 25);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error reading item_throw cost for " + type + " from config, using default value 25", e);
            return 25;
        }
    }
}