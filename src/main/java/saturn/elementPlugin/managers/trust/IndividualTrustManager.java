package saturn.elementPlugin.managers.trust;

import saturn.elementPlugin.ElementPlugin;
import saturn.elementPlugin.data.DataStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages individual trust relationships between players
 * Handles pending requests and mutual trust
 */
public class IndividualTrustManager {
    private final ElementPlugin plugin;
    private final DataStore store;

    // Individual trust relationships
    private final Map<UUID, Set<UUID>> trusted = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> pending = new ConcurrentHashMap<>();

    public IndividualTrustManager(ElementPlugin plugin) {
        this.plugin = plugin;
        this.store = plugin.getDataStore();
    }

    /**
     * Check if two players have mutual trust
     */
    public boolean areMutuallyTrusted(UUID player1, UUID player2) {
        return getTrusted(player1).contains(player2) &&
                getTrusted(player2).contains(player1);
    }

    /**
     * Get all players that this player trusts
     */
    public Set<UUID> getTrusted(UUID owner) {
        return trusted.computeIfAbsent(owner, store::getTrusted);
    }

    /**
     * Add one-way trust from owner to other
     */
    public void addTrust(UUID owner, UUID other) {
        Set<UUID> set = getTrusted(owner);
        set.add(other);
        store.setTrusted(owner, set);
    }

    /**
     * Add mutual trust between two players
     */
    public void addMutualTrust(UUID a, UUID b) {
        addTrust(a, b);
        addTrust(b, a);
    }

    /**
     * Remove one-way trust from owner to other
     */
    public void removeTrust(UUID owner, UUID other) {
        Set<UUID> set = getTrusted(owner);
        set.remove(other);
        store.setTrusted(owner, set);
    }

    /**
     * Remove mutual trust between two players
     */
    public void removeMutualTrust(UUID a, UUID b) {
        removeTrust(a, b);
        removeTrust(b, a);
    }

    /**
     * Add a pending trust request
     */
    public void addPending(UUID target, UUID requestor) {
        pending.computeIfAbsent(target, k -> ConcurrentHashMap.newKeySet()).add(requestor);
    }

    /**
     * Check if there's a pending request from requestor to target
     */
    public boolean hasPending(UUID target, UUID requestor) {
        return pending.getOrDefault(target, Set.of()).contains(requestor);
    }

    /**
     * Clear a pending request
     */
    public void clearPending(UUID target, UUID requestor) {
        Set<UUID> set = pending.get(target);
        if (set != null) {
            set.remove(requestor);
        }
    }

    /**
     * Get names of all trusted players for display
     */
    public List<String> getTrustedNames(UUID owner) {
        List<String> names = new ArrayList<>();
        for (UUID uuid : getTrusted(owner)) {
            Player player = Bukkit.getPlayer(uuid);
            names.add(player != null ? player.getName() : uuid.toString());
        }
        return names;
    }
}