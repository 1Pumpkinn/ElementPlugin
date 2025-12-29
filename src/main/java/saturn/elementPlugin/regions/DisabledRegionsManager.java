package saturn.elementPlugin.regions;

import saturn.elementPlugin.ElementPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages regions where abilities are disabled
 */
public class DisabledRegionsManager {
    private final ElementPlugin plugin;
    private final File regionsFile;
    private FileConfiguration regionsConfig;

    private final Map<String, DisabledRegion> regions = new ConcurrentHashMap<>();

    public DisabledRegionsManager(ElementPlugin plugin) {
        this.plugin = plugin;

        // Setup regions file
        this.regionsFile = new File(plugin.getDataFolder(), "disabled-regions.yml");

        if (!regionsFile.exists()) {
            try {
                regionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create disabled-regions.yml: " + e.getMessage());
            }
        }

        this.regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
        loadRegions();
    }

    /**
     * Load all regions from disk
     */
    private void loadRegions() {
        ConfigurationSection regionsSection = regionsConfig.getConfigurationSection("regions");

        if (regionsSection == null) {
            plugin.getLogger().info("No disabled regions found");
            return;
        }

        for (String regionName : regionsSection.getKeys(false)) {
            ConfigurationSection regionSection = regionsSection.getConfigurationSection(regionName);
            if (regionSection != null) {
                try {
                    DisabledRegion region = DisabledRegion.loadFrom(regionName, regionSection);
                    regions.put(regionName.toLowerCase(), region);
                    plugin.getLogger().info("Loaded disabled region: " + region);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load region '" + regionName + "': " + e.getMessage());
                }
            }
        }

        plugin.getLogger().info("Loaded " + regions.size() + " disabled region(s)");
    }

    /**
     * Save all regions to disk
     */
    private void saveRegions() {
        // Clear existing data
        regionsConfig.set("regions", null);

        ConfigurationSection regionsSection = regionsConfig.createSection("regions");

        for (DisabledRegion region : regions.values()) {
            ConfigurationSection regionSection = regionsSection.createSection(region.getName());
            region.saveTo(regionSection);
        }

        try {
            regionsConfig.save(regionsFile);
            plugin.getLogger().info("Saved " + regions.size() + " disabled region(s)");
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save disabled-regions.yml: " + e.getMessage());
        }
    }

    /**
     * Add a new disabled region
     */
    public boolean addRegion(String name, Location pos1, Location pos2) {
        if (regions.containsKey(name.toLowerCase())) {
            return false;
        }

        // Check if positions are in the same world
        if (!pos1.getWorld().equals(pos2.getWorld())) {
            return false;
        }

        DisabledRegion region = new DisabledRegion(name, pos1, pos2);
        regions.put(name.toLowerCase(), region);
        saveRegions();

        plugin.getLogger().info("Created disabled region: " + region);
        return true;
    }

    /**
     * Remove a disabled region
     */
    public boolean removeRegion(String name) {
        DisabledRegion removed = regions.remove(name.toLowerCase());

        if (removed != null) {
            saveRegions();
            plugin.getLogger().info("Removed disabled region: " + removed.getName());
            return true;
        }

        return false;
    }

    /**
     * Check if a location is inside any disabled region
     */
    public boolean isInDisabledRegion(Location location) {
        for (DisabledRegion region : regions.values()) {
            if (region.contains(location)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the name of the disabled region at a location (if any)
     */
    public String getRegionNameAt(Location location) {
        for (DisabledRegion region : regions.values()) {
            if (region.contains(location)) {
                return region.getName();
            }
        }
        return null;
    }

    /**
     * Check if abilities are disabled for a player at their current location
     */
    public boolean areAbilitiesDisabled(Player player) {
        return isInDisabledRegion(player.getLocation());
    }

    /**
     * Get a region by name
     */
    public DisabledRegion getRegion(String name) {
        return regions.get(name.toLowerCase());
    }

    /**
     * Get all region names
     */
    public Set<String> getRegionNames() {
        return new HashSet<>(regions.keySet());
    }

    /**
     * Get all regions
     */
    public Collection<DisabledRegion> getAllRegions() {
        return new ArrayList<>(regions.values());
    }

    /**
     * Check if a region exists
     */
    public boolean regionExists(String name) {
        return regions.containsKey(name.toLowerCase());
    }

    /**
     * Reload regions from disk
     */
    public void reload() {
        regions.clear();
        regionsConfig = YamlConfiguration.loadConfiguration(regionsFile);
        loadRegions();
    }
}