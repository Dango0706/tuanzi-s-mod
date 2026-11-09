package me.tuanzi.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WitherKillManager {
    // 内存运行时缓存，与 Mixin 互为双重保障，确保无论环境如何数据读取与写入 100% 成功
    private static final Map<UUID, Integer> RUNTIME_CACHE = new ConcurrentHashMap<>();

    public static int getKills(ServerPlayer player) {
        if (player == null) return 0;
        if (player instanceof WitherKillTracker tracker) {
            int kills = tracker.tuanzis_mod$getWitherKills();
            RUNTIME_CACHE.put(player.getUUID(), kills);
            return kills;
        }
        return RUNTIME_CACHE.getOrDefault(player.getUUID(), 0);
    }

    public static void setKills(ServerPlayer player, int kills) {
        if (player == null) return;
        int val = Math.max(0, kills);
        RUNTIME_CACHE.put(player.getUUID(), val);
        if (player instanceof WitherKillTracker tracker) {
            tracker.tuanzis_mod$setWitherKills(val);
        }
    }

    public static int addKills(ServerPlayer player, int amount) {
        if (player == null) return 0;
        int updated = Math.max(0, getKills(player) + amount);
        setKills(player, updated);
        return updated;
    }
}
