package me.tuanzi.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReturningSwanCooldownTracker {
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    public static long getNextAvailableTime(UUID uuid) {
        if (uuid == null) return 0L;
        return COOLDOWNS.getOrDefault(uuid, 0L);
    }

    public static void setNextAvailableTime(UUID uuid, long time) {
        if (uuid != null) {
            COOLDOWNS.put(uuid, time);
        }
    }

    public static void reset() {
        COOLDOWNS.clear();
    }
}
