package me.tuanzi.gacha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.tuanzi.util.ModLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PlayerGachaManager {
    private static final Path PLAYER_DIR = Path.of("config", "tuanzis_mod", "players");
    private static final Path BACKUP_ROOT_DIR = Path.of("config", "tuanzis_mod", "backups", "players");

    private static final Map<UUID, PlayerGachaState> cache = new ConcurrentHashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // 异步 30 秒合并落盘定时器
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "Gacha-Player-Saver");
                t.setDaemon(true);
                return t;
            }
    );

    static {
        // 每 30 秒执行一次异步脏数据轮询合并落盘
        scheduler.scheduleAtFixedRate(PlayerGachaManager::flushDirtyStatesAsync, 30, 30, TimeUnit.SECONDS);
    }

    public static void initialize() {
        try {
            if (!Files.exists(PLAYER_DIR)) {
                Files.createDirectories(PLAYER_DIR);
            }
        } catch (IOException e) {
            ModLog.debug("初始化玩家保底配置目录失败: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 🔍 获取或创建玩家保底及历史状态 (读缓存/磁盘)
    // ──────────────────────────────────────────────
    public static PlayerGachaState getOrCreatePlayerState(UUID uuid) {
        return cache.computeIfAbsent(uuid, k -> {
            Path filePath = PLAYER_DIR.resolve(uuid.toString() + ".json");
            if (Files.exists(filePath)) {
                try {
                    String content = Files.readString(filePath);
                    PlayerGachaState state = gson.fromJson(content, PlayerGachaState.class);
                    if (state != null) {
                        ModLog.debug("成功从磁盘装载玩家保底与历史: " + uuid);
                        return state;
                    }
                } catch (Exception e) {
                    ModLog.debug("解析玩家保底数据异常: " + uuid + "，错误: " + e.getMessage());
                }
            }
            
            // 磁盘不存在或损坏，则创建新的并落盘
            PlayerGachaState newState = new PlayerGachaState(uuid);
            saveStateSafe(newState);
            ModLog.debug("为新玩家创建保底与历史容器: " + uuid);
            return newState;
        });
    }

    // ──────────────────────────────────────────────
    // 🛡️ 脏状态异步刷盘
    // ──────────────────────────────────────────────
    private static void flushDirtyStatesAsync() {
        for (PlayerGachaState state : cache.values()) {
            if (state.isDirty()) {
                // 提交到异步线程池进行原子安全保存，防止任何 IO 卡死游戏主线程
                CompletableFuture.runAsync(() -> {
                    if (state.isDirty()) {
                        saveStateSafe(state);
                        backupStateSafe(state);
                        state.setDirty(false);
                    }
                });
            }
        }
    }

    // ──────────────────────────────────────────────
    // 💾 安全原子保存机制
    // ──────────────────────────────────────────────
    public static synchronized void saveStateSafe(PlayerGachaState state) {
        Path targetPath = PLAYER_DIR.resolve(state.getUuid().toString() + ".json");
        Path tempPath = PLAYER_DIR.resolve(state.getUuid().toString() + ".json.tmp");

        try {
            if (!Files.exists(PLAYER_DIR)) {
                Files.createDirectories(PLAYER_DIR);
            }

            String jsonString = gson.toJson(state);
            Files.writeString(tempPath, jsonString);
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            ModLog.debug("安全原子保存玩家 " + state.getUuid() + " 数据失败: " + e.getMessage());
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
        }
    }

    // ──────────────────────────────────────────────
    // 🚪 玩家断开连接监听 (强刷并移出缓存，防内存泄漏)
    // ──────────────────────────────────────────────
    public static void onPlayerDisconnect(UUID uuid) {
        PlayerGachaState state = cache.remove(uuid);
        if (state != null) {
            if (state.isDirty()) {
                saveStateSafe(state);
                backupStateSafe(state);
                state.setDirty(false);
            }
            ModLog.debug("玩家下线，安全刷盘保底数据并移出内存: " + uuid);
        }
    }

    // ──────────────────────────────────────────────
    // ⏱️ 个人保底数据滚动备份 (最近 10 份)
    // ──────────────────────────────────────────────
    public static void backupStateSafe(PlayerGachaState state) {
        Path playerBackupDir = BACKUP_ROOT_DIR.resolve(state.getUuid().toString());
        try {
            if (!Files.exists(playerBackupDir)) {
                Files.createDirectories(playerBackupDir);
            }

            DateTimeFormatter dft = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(dft);
            Path backupPath = playerBackupDir.resolve(state.getUuid().toString() + "_" + timestamp + ".json");
            Path tempBackupPath = playerBackupDir.resolve(state.getUuid().toString() + "_" + timestamp + ".json.tmp");

            String jsonString = gson.toJson(state);
            Files.writeString(tempBackupPath, jsonString);
            Files.move(tempBackupPath, backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // 滚动清理
            cleanOldBackups(playerBackupDir);
        } catch (IOException e) {
            ModLog.debug("备份玩家 " + state.getUuid() + " 数据失败: " + e.getMessage());
        }
    }

    private static void cleanOldBackups(Path playerBackupDir) {
        try {
            if (!Files.exists(playerBackupDir)) return;
            List<Path> backupFiles;
            try (var stream = Files.list(playerBackupDir)) {
                backupFiles = stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .sorted((p1, p2) -> {
                            try {
                                return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1)); // 降序
                            } catch (IOException e) {
                                return 0;
                            }
                        })
                        .collect(Collectors.toList());
            }

            if (backupFiles.size() > 10) {
                for (int i = 10; i < backupFiles.size(); i++) {
                    Files.deleteIfExists(backupFiles.get(i));
                }
                ModLog.debug("已清理过期玩家保底备份，保留最新 10 份。");
            }
        } catch (Exception e) {
            ModLog.debug("滚动清理玩家保底快照发生异常: " + e.getMessage());
        }
    }

    public static void saveAllCachedStates() {
        ModLog.debug("服务器关闭，开始强制同步保存所有玩家的抽卡定轨状态...");
        for (PlayerGachaState state : cache.values()) {
            saveStateSafe(state);
            backupStateSafe(state);
            state.setDirty(false);
        }
        ModLog.debug("玩家抽卡定轨数据强制保存完成。");
    }

    public static void shutdown() {
        ModLog.debug("正在关闭 PlayerGachaManager 调度器...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        saveAllCachedStates();
    }
}
