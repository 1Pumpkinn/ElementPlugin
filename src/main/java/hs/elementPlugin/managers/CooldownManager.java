package hs.elementPlugin.managers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final Map<String, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(UUID uuid, String key) {
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(uuid + ":" + key);
        return until != null && until > now;
    }

    public long msRemaining(UUID uuid, String key) {
        long now = System.currentTimeMillis();
        Long until = cooldowns.get(uuid + ":" + key);
        return Math.max(0L, until == null ? 0L : until - now);
    }

    public void set(UUID uuid, String key, long millis) {
        cooldowns.put(uuid + ":" + key, System.currentTimeMillis() + millis);
    }
}