package saturn.elementPlugin.data;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

/**
 * Stores trust relationships for a player
 * Tracks who this player trusts and who trusts them
 */
public class TrustData {
    private final UUID playerUuid;

    // Players this player trusts
    private final Set<UUID> trustedPlayers = new HashSet<>();

    // Pending trust requests this player has sent to others
    private final Map<UUID, Long> pendingOutgoing = new HashMap<>();

    // Pending trust requests this player has received from others
    private final Map<UUID, Long> pendingIncoming = new HashMap<>();

    // Expiry time for pending requests (5 minutes)
    private static final long REQUEST_EXPIRY_MS = 5 * 60 * 1000L;

    private transient boolean dirty = false;

    public TrustData(UUID playerUuid) {
        this.playerUuid = playerUuid;
    }

    /**
     * Load trust data from configuration
     */
    public TrustData(UUID playerUuid, ConfigurationSection section) {
        this.playerUuid = playerUuid;

        if (section == null) return;

        // Load trusted players
        List<String> trustedList = section.getStringList("trusted");
        for (String uuidStr : trustedList) {
            try {
                trustedPlayers.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {}
        }

        // Load pending outgoing requests
        ConfigurationSection outgoing = section.getConfigurationSection("pending-outgoing");
        if (outgoing != null) {
            for (String key : outgoing.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long timestamp = outgoing.getLong(key);
                    if (System.currentTimeMillis() - timestamp < REQUEST_EXPIRY_MS) {
                        pendingOutgoing.put(uuid, timestamp);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        // Load pending incoming requests
        ConfigurationSection incoming = section.getConfigurationSection("pending-incoming");
        if (incoming != null) {
            for (String key : incoming.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    long timestamp = incoming.getLong(key);
                    if (System.currentTimeMillis() - timestamp < REQUEST_EXPIRY_MS) {
                        pendingIncoming.put(uuid, timestamp);
                    }
                } catch (IllegalArgumentException ignored) {}
            }
        }

        this.dirty = false;
    }

    // ========================================
    // TRUST MANAGEMENT
    // ========================================

    /**
     * Check if this player trusts another player
     */
    public boolean trusts(UUID other) {
        return trustedPlayers.contains(other);
    }

    /**
     * Add a trusted player
     */
    public void addTrust(UUID other) {
        if (trustedPlayers.add(other)) {
            markDirty();
        }
    }

    /**
     * Remove a trusted player
     */
    public void removeTrust(UUID other) {
        if (trustedPlayers.remove(other)) {
            markDirty();
        }
    }

    /**
     * Get all trusted players
     */
    public Set<UUID> getTrustedPlayers() {
        return new HashSet<>(trustedPlayers);
    }

    // ========================================
    // PENDING REQUESTS
    // ========================================

    /**
     * Add a pending outgoing trust request
     */
    public void addPendingOutgoing(UUID target) {
        pendingOutgoing.put(target, System.currentTimeMillis());
        markDirty();
    }

    /**
     * Add a pending incoming trust request
     */
    public void addPendingIncoming(UUID sender) {
        pendingIncoming.put(sender, System.currentTimeMillis());
        markDirty();
    }

    /**
     * Remove a pending outgoing request
     */
    public void removePendingOutgoing(UUID target) {
        if (pendingOutgoing.remove(target) != null) {
            markDirty();
        }
    }

    /**
     * Remove a pending incoming request
     */
    public void removePendingIncoming(UUID sender) {
        if (pendingIncoming.remove(sender) != null) {
            markDirty();
        }
    }

    /**
     * Check if there's a pending outgoing request to a player
     */
    public boolean hasPendingOutgoing(UUID target) {
        Long timestamp = pendingOutgoing.get(target);
        if (timestamp == null) return false;

        // Check if expired
        if (System.currentTimeMillis() - timestamp > REQUEST_EXPIRY_MS) {
            pendingOutgoing.remove(target);
            markDirty();
            return false;
        }

        return true;
    }

    /**
     * Check if there's a pending incoming request from a player
     */
    public boolean hasPendingIncoming(UUID sender) {
        Long timestamp = pendingIncoming.get(sender);
        if (timestamp == null) return false;

        // Check if expired
        if (System.currentTimeMillis() - timestamp > REQUEST_EXPIRY_MS) {
            pendingIncoming.remove(sender);
            markDirty();
            return false;
        }

        return true;
    }

    /**
     * Get all pending incoming requests
     */
    public Set<UUID> getPendingIncoming() {
        // Clean up expired requests
        Set<UUID> expired = new HashSet<>();
        long now = System.currentTimeMillis();

        for (Map.Entry<UUID, Long> entry : pendingIncoming.entrySet()) {
            if (now - entry.getValue() > REQUEST_EXPIRY_MS) {
                expired.add(entry.getKey());
            }
        }

        for (UUID uuid : expired) {
            pendingIncoming.remove(uuid);
        }

        if (!expired.isEmpty()) {
            markDirty();
        }

        return new HashSet<>(pendingIncoming.keySet());
    }

    /**
     * Clean up expired requests
     */
    public void cleanupExpired() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        // Clean outgoing
        Set<UUID> expiredOut = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : pendingOutgoing.entrySet()) {
            if (now - entry.getValue() > REQUEST_EXPIRY_MS) {
                expiredOut.add(entry.getKey());
            }
        }
        for (UUID uuid : expiredOut) {
            pendingOutgoing.remove(uuid);
            changed = true;
        }

        // Clean incoming
        Set<UUID> expiredIn = new HashSet<>();
        for (Map.Entry<UUID, Long> entry : pendingIncoming.entrySet()) {
            if (now - entry.getValue() > REQUEST_EXPIRY_MS) {
                expiredIn.add(entry.getKey());
            }
        }
        for (UUID uuid : expiredIn) {
            pendingIncoming.remove(uuid);
            changed = true;
        }

        if (changed) {
            markDirty();
        }
    }

    // ========================================
    // DIRTY FLAG
    // ========================================

    public boolean isDirty() {
        return dirty;
    }

    private void markDirty() {
        this.dirty = true;
    }

    public void markClean() {
        this.dirty = false;
    }

    // ========================================
    // SERIALIZATION
    // ========================================

    /**
     * Save to configuration section
     */
    public void saveTo(ConfigurationSection section) {
        // Save trusted players
        List<String> trustedList = new ArrayList<>();
        for (UUID uuid : trustedPlayers) {
            trustedList.add(uuid.toString());
        }
        section.set("trusted", trustedList);

        // Save pending outgoing
        ConfigurationSection outgoing = section.createSection("pending-outgoing");
        for (Map.Entry<UUID, Long> entry : pendingOutgoing.entrySet()) {
            outgoing.set(entry.getKey().toString(), entry.getValue());
        }

        // Save pending incoming
        ConfigurationSection incoming = section.createSection("pending-incoming");
        for (Map.Entry<UUID, Long> entry : pendingIncoming.entrySet()) {
            incoming.set(entry.getKey().toString(), entry.getValue());
        }

        markClean();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }
}