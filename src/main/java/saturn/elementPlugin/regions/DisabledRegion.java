package saturn.elementPlugin.regions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Represents a cuboid region where abilities are disabled
 */
public class DisabledRegion {
    private final String name;
    private final String worldName;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public DisabledRegion(String name, Location pos1, Location pos2) {
        this.name = name;
        this.worldName = pos1.getWorld().getName();

        // Calculate min/max coordinates
        this.minX = Math.min(pos1.getBlockX(), pos2.getBlockX());
        this.minY = Math.min(pos1.getBlockY(), pos2.getBlockY());
        this.minZ = Math.min(pos1.getBlockZ(), pos2.getBlockZ());

        this.maxX = Math.max(pos1.getBlockX(), pos2.getBlockX());
        this.maxY = Math.max(pos1.getBlockY(), pos2.getBlockY());
        this.maxZ = Math.max(pos1.getBlockZ(), pos2.getBlockZ());
    }

    public DisabledRegion(String name, String worldName, int minX, int minY, int minZ,
                          int maxX, int maxY, int maxZ) {
        this.name = name;
        this.worldName = worldName;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    /**
     * Check if a location is inside this region
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(worldName)) {
            return false;
        }

        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    // Getters
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }

    /**
     * Save this region to a configuration section
     */
    public void saveTo(ConfigurationSection section) {
        section.set("world", worldName);
        section.set("minX", minX);
        section.set("minY", minY);
        section.set("minZ", minZ);
        section.set("maxX", maxX);
        section.set("maxY", maxY);
        section.set("maxZ", maxZ);
    }

    /**
     * Load a region from a configuration section
     */
    public static DisabledRegion loadFrom(String name, ConfigurationSection section) {
        String world = section.getString("world");
        int minX = section.getInt("minX");
        int minY = section.getInt("minY");
        int minZ = section.getInt("minZ");
        int maxX = section.getInt("maxX");
        int maxY = section.getInt("maxY");
        int maxZ = section.getInt("maxZ");

        return new DisabledRegion(name, world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * Get volume of the region in blocks
     */
    public int getVolume() {
        return (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
    }

    @Override
    public String toString() {
        return name + " [" + worldName + " " +
                minX + "," + minY + "," + minZ + " -> " +
                maxX + "," + maxY + "," + maxZ + "]";
    }
}