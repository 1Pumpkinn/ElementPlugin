package saturn.elementPlugin.util;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * WorldGuard integration for preventing ability usage in protected regions
 * Blocks abilities if PVP is disabled in any region the player or target is in
 */
public final class WorldGuardIntegration {

    private static boolean worldGuardEnabled = false;

    /**
     * Initialize WorldGuard integration
     * Call this in onEnable()
     */
    public static void initialize(Plugin plugin) {
        try {
            Plugin wg = plugin.getServer().getPluginManager().getPlugin("WorldGuard");
            if (wg != null && wg.isEnabled()) {
                // Test if WorldGuard API is accessible
                if (WorldGuard.getInstance() != null) {
                    worldGuardEnabled = true;
                    plugin.getLogger().info("[ElementPlugin] WorldGuard integration enabled");
                } else {
                    worldGuardEnabled = false;
                    plugin.getLogger().warning("[ElementPlugin] WorldGuard API not accessible");
                }
            } else {
                worldGuardEnabled = false;
                plugin.getLogger().info("[ElementPlugin] WorldGuard not found, PVP protection disabled");
            }
        } catch (Exception e) {
            worldGuardEnabled = false;
            plugin.getLogger().warning("[ElementPlugin] Failed to initialize WorldGuard: " + e.getMessage());
        }
    }

    /**
     * Check if PVP is allowed at a location
     * Returns false if PVP is denied (abilities should be blocked)
     * Returns true if PVP is allowed or not set (abilities allowed)
     */
    private static boolean isPvPAllowed(Location location) {
        if (!worldGuardEnabled || location == null) {
            return true; // No WorldGuard = allow abilities
        }

        try {
            RegionContainer container = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer();

            if (container == null) {
                return true;
            }

            // Get region manager for the world - convert Bukkit World to WorldEdit World
            // Note: Casting works in WorldGuard 7.0.9 / WorldEdit 7.3.9 due to bridge compatibility
            World wgWorld = (World) location.getWorld();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = container.get(wgWorld);
            if (regionManager == null) {
                return true;
            }

            com.sk89q.worldedit.math.BlockVector3 blockVector = BukkitAdapter.asBlockVector(location);
            ApplicableRegionSet regions = regionManager.getApplicableRegions(blockVector);

            // No regions = PVP allowed
            if (regions == null || regions.size() == 0) {
                return true;
            }

            // Check PVP flag state
            StateFlag.State pvpState = regions.queryState(null, Flags.PVP);
            
            // If PVP is explicitly denied, block abilities
            return pvpState != StateFlag.State.DENY;
        } catch (Exception e) {
            // Error checking = allow abilities (fail open)
            return true;
        }
    }

    /**
     * Check if a player can use abilities at their current location
     * @param player The player trying to use an ability
     * @return true if allowed, false if blocked
     */
    public static boolean canUseAbility(Player player) {
        if (player == null) return false;
        return isPvPAllowed(player.getLocation());
    }


    /**
     * Check if a player can use an ability on a target player
     * Both locations must allow PVP for the ability to work
     * @param player The player using the ability
     * @param target The target player
     * @return true if both locations allow PVP, false otherwise
     */
    public static boolean canUseAbilityOnTarget(Player player, Player target) {
        if (player == null || target == null) return false;
        
        // Both locations must allow PVP
        boolean playerLocationOK = isPvPAllowed(player.getLocation());
        boolean targetLocationOK = isPvPAllowed(target.getLocation());
        
        return playerLocationOK && targetLocationOK;
    }

    /**
     * Get the name of a protected region that's blocking PVP at a location
     * @param location The location to check
     * @return Region name if PVP is denied, null otherwise
     */
    public static String getProtectedRegionName(Location location) {
        if (!worldGuardEnabled || location == null) {
            return null;
        }

        try {
            RegionContainer container = WorldGuard.getInstance()
                    .getPlatform()
                    .getRegionContainer();

            if (container == null) return null;

            // Get region manager for the world - convert Bukkit World to WorldEdit World
            // Note: Casting works in WorldGuard 7.0.9 / WorldEdit 7.3.9 due to bridge compatibility
            World wgWorld = (World) location.getWorld();
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = container.get(wgWorld);
            if (regionManager == null) {
                return null;
            }

            com.sk89q.worldedit.math.BlockVector3 blockVector = BukkitAdapter.asBlockVector(location);
            ApplicableRegionSet regions = regionManager.getApplicableRegions(blockVector);

            if (regions == null || regions.size() == 0) {
                return null;
            }

            // Find the first region that denies PVP
            for (com.sk89q.worldguard.protection.regions.ProtectedRegion region : regions) {
                StateFlag.State pvpState = region.getFlag(Flags.PVP);
                if (pvpState == StateFlag.State.DENY) {
                    return region.getId();
                }
            }
        } catch (Exception e) {
            // Error = no region name
        }

        return null;
    }

    /**
     * Send a protection message to a player
     * @param player The player to notify
     */
    public static void sendProtectionMessage(Player player) {
        if (player == null) return;
        
        String region = getProtectedRegionName(player.getLocation());
        if (region != null) {
            player.sendMessage("§cYou cannot use abilities in region: §f" + region);
        } else {
            player.sendMessage("§cYou cannot use abilities in this protected region!");
        }
    }

    /**
     * Send a message when targeting a protected player
     * @param player The player using the ability
     * @param target The protected target
     */
    public static void sendTargetProtectionMessage(Player player, Player target) {
        if (player == null || target == null) return;
        
        String region = getProtectedRegionName(target.getLocation());
        if (region != null) {
            player.sendMessage("§cThat player is protected by region: §f" + region);
        } else {
            player.sendMessage("§cThat player is in a protected region!");
        }
    }
}
