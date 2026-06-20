package me.tuanzi.gacha;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tuanzi.util.ModLog;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class PoolManager {
    private static final Path POOL_DIR = Path.of("config", "tuanzis_mod", "pools");
    private static final Path BACKUP_DIR = Path.of("config", "tuanzis_mod", "backups", "pools");

    private static final Map<String, GachaPool> pools = new ConcurrentHashMap<>();
    
    // 防抖定时器线程池
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "Gacha-Pool-Saver");
                t.setDaemon(true);
                return t;
            }
    );
    private static final Map<String, ScheduledFuture<?>> pendingSaves = new ConcurrentHashMap<>();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static GachaPool getPool(String poolId) {
        return pools.get(poolId);
    }

    public static Map<String, GachaPool> getPools() {
        return Collections.unmodifiableMap(pools);
    }

    // ──────────────────────────────────────────────
    // 📁 加载所有卡池数据
    // ──────────────────────────────────────────────
    public static synchronized void loadAllPools(HolderLookup.Provider registries) {
        try {
            if (!Files.exists(POOL_DIR)) {
                Files.createDirectories(POOL_DIR);
            }
            
            // 扫描 POOL_DIR 下所有 json
            List<Path> jsonFiles;
            try (var stream = Files.list(POOL_DIR)) {
                jsonFiles = stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .collect(Collectors.toList());
            }

            if (jsonFiles.isEmpty()) {
                // 如果为空，生成默认的常驻与限定模板，避免玩家无参照配置
                generateDefaultTemplates(registries);
                try (var stream = Files.list(POOL_DIR)) {
                    jsonFiles = stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .collect(Collectors.toList());
                }
            }

            pools.clear();
            for (Path path : jsonFiles) {
                try {
                    String content = Files.readString(path);
                    JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                    GachaPool pool = GachaPool.fromJson(json, registries);
                    pools.put(pool.getPoolId(), pool);
                    ModLog.debug("成功加载抽卡卡池: " + pool.getPoolName() + " (ID: " + pool.getPoolId() + ")");
                } catch (Exception e) {
                    ModLog.debug("解析卡池文件失败: " + path.getFileName() + "，错误: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            ModLog.debug("加载卡池文件夹异常: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 🛡️ 安全文件保存机制 (Atomic Move)
    // ──────────────────────────────────────────────
    public static synchronized void savePoolSafe(GachaPool pool, HolderLookup.Provider registries) {
        Path targetPath = POOL_DIR.resolve(pool.getPoolId() + ".json");
        Path tempPath = POOL_DIR.resolve(pool.getPoolId() + ".json.tmp");

        try {
            // 序列化
            JsonObject json = pool.toJson(registries);
            String prettyJson = gson.toJson(json);

            // 1. 先写入临时文件
            Files.writeString(tempPath, prettyJson);

            // 2. 原子性移动替换
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            
            // 更新内存
            pools.put(pool.getPoolId(), pool);
            ModLog.debug("卡池 " + pool.getPoolName() + " (" + pool.getPoolId() + ") 安全原子保存成功！");
        } catch (IOException e) {
            ModLog.debug("安全保存卡池 " + pool.getPoolId() + " 发生 IOException: " + e.getMessage());
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
        }
    }

    // ──────────────────────────────────────────────
    // ⏱️ 防抖异步延迟保存
    // ──────────────────────────────────────────────
    public static void scheduleDelayedSave(GachaPool pool, HolderLookup.Provider registries, Runnable onCompleteCallback) {
        String poolId = pool.getPoolId();
        
        // 如果先前有待保存的任务，立刻取消以重置定时器 (Debounce)
        ScheduledFuture<?> oldFuture = pendingSaves.remove(poolId);
        if (oldFuture != null) {
            oldFuture.cancel(false);
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                savePoolSafe(pool, registries);
                backupPoolSafe(pool, registries);
                pendingSaves.remove(poolId);
                
                if (onCompleteCallback != null) {
                    onCompleteCallback.run();
                }
            } catch (Exception e) {
                ModLog.debug("执行卡池防抖延迟写入失败: " + e.getMessage());
            }
        }, 5, TimeUnit.SECONDS);

        pendingSaves.put(poolId, future);
    }

    // ──────────────────────────────────────────────
    // ⏱️ 安全滚动备份 (最近 10 份)
    // ──────────────────────────────────────────────
    public static void backupPoolSafe(GachaPool pool, HolderLookup.Provider registries) {
        try {
            if (!Files.exists(BACKUP_DIR)) {
                Files.createDirectories(BACKUP_DIR);
            }

            DateTimeFormatter dft = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
            String timestamp = LocalDateTime.now().format(dft);
            Path backupPath = BACKUP_DIR.resolve(pool.getPoolId() + "_" + timestamp + ".json");
            Path tempBackupPath = BACKUP_DIR.resolve(pool.getPoolId() + "_" + timestamp + ".json.tmp");

            JsonObject json = pool.toJson(registries);
            String prettyJson = gson.toJson(json);

            Files.writeString(tempBackupPath, prettyJson);
            Files.move(tempBackupPath, backupPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // 滚动清理，最多保留 10 份
            cleanOldBackups();
        } catch (IOException e) {
            ModLog.debug("备份卡池 " + pool.getPoolId() + " 失败: " + e.getMessage());
        }
    }

    private static void cleanOldBackups() {
        try {
            if (!Files.exists(BACKUP_DIR)) return;
            List<Path> backupFiles;
            try (var stream = Files.list(BACKUP_DIR)) {
                backupFiles = stream.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .sorted((p1, p2) -> {
                            try {
                                return Files.getLastModifiedTime(p2).compareTo(Files.getLastModifiedTime(p1)); // 降序，最新的在最前
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
                ModLog.debug("已清理过期卡池备份，保留最新的 10 份。");
            }
        } catch (Exception e) {
            ModLog.debug("滚动清理卡池备份文件发生异常: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────
    // 默认卡池模板生成器
    // ──────────────────────────────────────────────
    private static void generateDefaultTemplates(HolderLookup.Provider registries) {
        // 1. 常驻池模板
        GachaPool normalPool = new GachaPool("normal", "星旅常驻卡池");
        normalPool.getLegendary().add(new GachaPoolItem("netherite_sword", 10, 
                List.of("say 恭喜玩家 %player% 抽中了常驻传说神兵！"), new ItemStack(Items.NETHERITE_SWORD)));
        normalPool.getEpic().add(new GachaPoolItem("diamond_chestplate", 10, new ArrayList<>(), new ItemStack(Items.DIAMOND_CHESTPLATE)));
        normalPool.getRare().add(new GachaPoolItem("golden_apple", 15, new ArrayList<>(), new ItemStack(Items.GOLDEN_APPLE)));
        normalPool.getUncommon().add(new GachaPoolItem("iron_ingot", 23, new ArrayList<>(), new ItemStack(Items.IRON_INGOT)));
        normalPool.getCommon().add(new GachaPoolItem("oak_planks", 50, new ArrayList<>(), new ItemStack(Items.OAK_PLANKS)));
        savePoolSafe(normalPool, registries);

        // 2. 限定池模板
        GachaPool sakuraPool = new GachaPool("sakura_moon", "樱花祭限定卡池");
        sakuraPool.getLegendary().add(new GachaPoolItem("elytra", 10, 
                List.of("say 恭喜玩家 %player% 抽中了限定传说鞘翅！"), new ItemStack(Items.ELYTRA)));
        sakuraPool.getEpic().add(new GachaPoolItem("netherite_upgrade_smithing_template", 10, new ArrayList<>(), new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)));
        sakuraPool.getRare().add(new GachaPoolItem("ender_eye", 15, new ArrayList<>(), new ItemStack(Items.ENDER_EYE)));
        sakuraPool.getUncommon().add(new GachaPoolItem("lapis_lazuli", 23, new ArrayList<>(), new ItemStack(Items.LAPIS_LAZULI)));
        sakuraPool.getCommon().add(new GachaPoolItem("cobblestone", 50, new ArrayList<>(), new ItemStack(Items.COBBLESTONE)));
        savePoolSafe(sakuraPool, registries);
    }

    public static void shutdown(HolderLookup.Provider registries) {
        ModLog.debug("正在关闭 PoolManager 调度器...");
        
        // 立即同步落盘所有待保存的卡池，防止数据丢失
        for (Map.Entry<String, ScheduledFuture<?>> entry : pendingSaves.entrySet()) {
            entry.getValue().cancel(false);
            GachaPool pool = pools.get(entry.getKey());
            if (pool != null) {
                savePoolSafe(pool, registries);
                backupPoolSafe(pool, registries);
            }
        }
        pendingSaves.clear();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
