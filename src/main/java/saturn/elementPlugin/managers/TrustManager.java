package saturn.elementPlugin.managers;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.TrustData;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages the trust system for players
 * Handles trust requests, confirmations, and mutual trust checks
 */
public class TrustManager {
    private final ElementPlugin plugin;
    private final File trustFile;
    private FileConfiguration trustConfig;

    // Cache for trust data
    private final Map<UUID, TrustData> cache = new ConcurrentHashMap<>();

    public TrustManager(ElementPlugin plugin) {
        this.plugin = plugin;

        // Setup trust file
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }

        this.trustFile = new File(dataDir, "trust.yml");
        if (!trustFile.exists()) {
            try {
                trustFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create trust.yml: " + e.getMessage());
            }
        }

        this.trustConfig = YamlConfiguration.loadConfiguration(trustFile);

        // Start periodic cleanup of expired requests
        startCleanupTask();
    }

    /**
     * Start a task that periodically cleans up expired trust requests
     */
    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            for (TrustData data : cache.values()) {
                data.cleanupExpired();
                if (data.isDirty()) {
                    save(data);
                }
            }
        }, 6000L, 6000L); // Every 5 minutes
    }

    /**
     * Get trust data for a player
     */
    public TrustData getTrustData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    /**
     * Load trust data from disk
     */
    private TrustData loadFromDisk(UUID uuid) {
        try {
            trustConfig = YamlConfiguration.loadConfiguration(trustFile);

            String path = "players." + uuid.toString();
            ConfigurationSection section = trustConfig.getConfigurationSection(path);

            return new TrustData(uuid, section);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load trust data for " + uuid, e);
            return new TrustData(uuid);
        }
    }

    /**
     * Save trust data to disk
     */
    public void save(TrustData data) {
        if (data == null) return;

        try {
            trustConfig = YamlConfiguration.loadConfiguration(trustFile);

            String path = "players." + data.getPlayerUuid().toString();
            ConfigurationSection section = trustConfig.getConfigurationSection(path);

            if (section == null) {
                section = trustConfig.createSection(path);
            }

            data.saveTo(section);
            trustConfig.save(trustFile);

            cache.put(data.getPlayerUuid(), data);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save trust data for " + data.getPlayerUuid(), e);
        }
    }

    /**
     * Save all cached trust data
     */
    public void saveAll() {
        for (TrustData data : cache.values()) {
            if (data.isDirty()) {
                save(data);
            }
        }
    }

    // ========================================
    // TRUST OPERATIONS
    // ========================================

    /**
     * Check if player1 trusts player2
     */
    public boolean trusts(UUID player1, UUID player2) {
        TrustData data = getTrustData(player1);
        return data.trusts(player2);
    }

    /**
     * Check if player1 trusts player2 (by Player objects)
     */
    public boolean trusts(Player player1, Player player2) {
        return trusts(player1.getUniqueId(), player2.getUniqueId());
    }

    /**
     * Check if two players mutually trust each other
     */
    public boolean mutualTrust(UUID player1, UUID player2) {
        return trusts(player1, player2) && trusts(player2, player1);
    }

    /**
     * Check if two players mutually trust each other (by Player objects)
     */
    public boolean mutualTrust(Player player1, Player player2) {
        return mutualTrust(player1.getUniqueId(), player2.getUniqueId());
    }

    /**
     * Send a trust request from sender to target
     * @return true if request was sent, false if already exists or invalid
     */
    public boolean sendTrustRequest(UUID sender, UUID target) {
        // Can't trust yourself
        if (sender.equals(target)) {
            return false;
        }

        TrustData senderData = getTrustData(sender);
        TrustData targetData = getTrustData(target);

        // Check if already trusted
        if (senderData.trusts(target)) {
            return false;
        }

        // Check if there's already a pending request
        if (senderData.hasPendingOutgoing(target)) {
            return false;
        }

        // Add pending request
        senderData.addPendingOutgoing(target);
        targetData.addPendingIncoming(sender);

        save(senderData);
        save(targetData);

        return true;
    }

    /**
     * Accept a trust request
     * @return true if request was accepted, false if no pending request
     */
    public boolean acceptTrustRequest(UUID accepter, UUID requester) {
        TrustData accepterData = getTrustData(accepter);
        TrustData requesterData = getTrustData(requester);

        // Check if there's a pending request
        if (!accepterData.hasPendingIncoming(requester)) {
            return false;
        }

        // Add trust (only accepter trusts requester)
        accepterData.addTrust(requester);

        // Remove pending requests
        accepterData.removePendingIncoming(requester);
        requesterData.removePendingOutgoing(accepter);

        save(accepterData);
        save(requesterData);

        return true;
    }

    /**
     * Deny a trust request
     */
    public boolean denyTrustRequest(UUID denier, UUID requester) {
        TrustData denierData = getTrustData(denier);
        TrustData requesterData = getTrustData(requester);

        // Check if there's a pending request
        if (!denierData.hasPendingIncoming(requester)) {
            return false;
        }

        // Remove pending requests
        denierData.removePendingIncoming(requester);
        requesterData.removePendingOutgoing(denier);

        save(denierData);
        save(requesterData);

        return true;
    }

    /**
     * Remove trust (one-way)
     */
    public void removeTrust(UUID player, UUID trusted) {
        TrustData data = getTrustData(player);
        data.removeTrust(trusted);
        save(data);
    }

    /**
     * Get all players that a player trusts
     */
    public Set<UUID> getTrustedPlayers(UUID player) {
        return getTrustData(player).getTrustedPlayers();
    }

    /**
     * Get all pending incoming trust requests for a player
     */
    public Set<UUID> getPendingRequests(UUID player) {
        return getTrustData(player).getPendingIncoming();
    }

    /**
     * Clear cache for a player
     */
    public void invalidateCache(UUID player) {
        cache.remove(player);
    }
}