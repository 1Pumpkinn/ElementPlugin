package hs.elementSmpUtility.storage.pedestal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * YAML-based backup storage for pedestals
 * Provides redundancy alongside chunk PDC storage
 */
public class PedestalYmlStorage {

    private final JavaPlugin plugin;
    private final File dataFile;
    private FileConfiguration config;

    public PedestalYmlStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "pedestals.yml");
        loadConfig();
    }

    private void loadConfig() {
        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create pedestals.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Save pedestal data to YAML
     */
    public void savePedestal(Location location, ItemStack item) {
        String key = getLocationKey(location);

        if (item == null || item.getType().isAir()) {
            config.set(key, null);
        } else {
            config.set(key + ".item", item);
            config.set(key + ".world", location.getWorld().getName());
            config.set(key + ".x", location.getBlockX());
            config.set(key + ".y", location.getBlockY());
            config.set(key + ".z", location.getBlockZ());
        }

        saveConfig();
    }

    /**
     * Load pedestal data from YAML
     */
    public ItemStack loadPedestal(Location location) {
        String key = getLocationKey(location);

        if (!config.contains(key + ".item")) {
            return null;
        }

        return config.getItemStack(key + ".item");
    }

    /**
     * Remove pedestal from YAML
     */
    public void removePedestal(Location location) {
        String key = getLocationKey(location);
        config.set(key, null);
        saveConfig();
    }

    /**
     * Get all pedestal locations from YAML
     */
    public Map<Location, ItemStack> getAllPedestals() {
        Map<Location, ItemStack> pedestals = new HashMap<>();

        Set<String> keys = config.getKeys(false);
        for (String key : keys) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section != null) {
                String worldName = section.getString("world");
                World world = Bukkit.getWorld(worldName);

                if (world != null) {
                    int x = section.getInt("x");
                    int y = section.getInt("y");
                    int z = section.getInt("z");
                    Location loc = new Location(world, x, y, z);

                    ItemStack item = section.getItemStack("item");
                    if (item != null) {
                        pedestals.put(loc, item);
                    }
                }
            }
        }

        return pedestals;
    }

    /**
     * Save config to file
     */
    private void saveConfig() {
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save pedestals.yml: " + e.getMessage());
        }
    }

    /**
     * Create location key
     */
    private String getLocationKey(Location loc) {
        return loc.getWorld().getName() + "_" +
                loc.getBlockX() + "_" +
                loc.getBlockY() + "_" +
                loc.getBlockZ();
    }

    /**
     * Reload config from file
     */
    public void reload() {
        config = YamlConfiguration.loadConfiguration(dataFile);
    }

    /**
     * Get count of stored pedestals
     */
    public int getPedestalCount() {
        return config.getKeys(false).size();
    }
}