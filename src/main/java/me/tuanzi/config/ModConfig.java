package me.tuanzi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.tuanzi.util.ModLog;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模组全局配置管理（持久化至 config/tuanzis_mod/config.json）。
 */
public final class ModConfig {
    public static final int MIN_OVERWORLD_MAX_BUILD_HEIGHT = 320;
    public static final int MAX_OVERWORLD_MAX_BUILD_HEIGHT = 2032;
    public static final int DEFAULT_OVERWORLD_MAX_BUILD_HEIGHT = MAX_OVERWORLD_MAX_BUILD_HEIGHT;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ConfigData current = new ConfigData();
    private static volatile long lastSelfWriteNanos = 0L;
    private static volatile String lastKnownJsonContent = null;

    private ModConfig() {
    }

    public static int clampOverworldMaxBuildHeight(int height) {
        return Math.max(MIN_OVERWORLD_MAX_BUILD_HEIGHT, Math.min(MAX_OVERWORLD_MAX_BUILD_HEIGHT, height));
    }

    public static long getLastSelfWriteNanos() {
        return lastSelfWriteNanos;
    }

    public static String getLastKnownJsonContent() {
        return lastKnownJsonContent;
    }

    public static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("tuanzis_mod")
                .resolve("config.json");
    }

    public static void load() {
        load(getConfigPath());
    }

    public static synchronized void load(Path path) {
        try {
            if (!Files.exists(path)) {
                current = new ConfigData();
                save(path);
                ModLog.debug("未检测到模组主配置文件，已生成默认配置: " + path
                        + " (enableCasinoRecipes=" + current.enableCasinoRecipes
                        + ", overworldMaxBuildHeight=" + current.overworldMaxBuildHeight + ")");
                return;
            }

            String raw = Files.readString(path, StandardCharsets.UTF_8);
            JsonElement root = JsonParser.parseString(raw);
            if (!root.isJsonObject()) {
                ModLog.warn("[ModConfig] 配置文件根元素不是 JSON 对象，已使用默认值: " + path);
                current = new ConfigData();
                return;
            }

            JsonObject obj = root.getAsJsonObject();
            ConfigData loaded = GSON.fromJson(obj, ConfigData.class);
            if (loaded == null) {
                loaded = new ConfigData();
            }
            boolean needsSave = false;
            if (loaded.traumaHudConflictMode == null) {
                loaded.traumaHudConflictMode = TraumaHudConflictMode.ABOVE_CONFLICT;
                needsSave = true;
            }
            int clampedHeight = clampOverworldMaxBuildHeight(loaded.overworldMaxBuildHeight);
            if (clampedHeight != loaded.overworldMaxBuildHeight) {
                ModLog.debug("[ModConfig] overworldMaxBuildHeight 超出允许范围 ["
                        + MIN_OVERWORLD_MAX_BUILD_HEIGHT + ", " + MAX_OVERWORLD_MAX_BUILD_HEIGHT
                        + "]，已钳制: " + loaded.overworldMaxBuildHeight + " -> " + clampedHeight);
                loaded.overworldMaxBuildHeight = clampedHeight;
                needsSave = true;
            }
            if (sanitizeCanvasConfig(loaded)) {
                needsSave = true;
            }
            current = loaded;
            lastKnownJsonContent = raw;

            if (!obj.has("enableCasinoRecipes")
                    || !obj.has("traumaHudConflictMode")
                    || !obj.has("overworldMaxBuildHeight")
                    || !hasAllCanvasFields(obj)
                    || needsSave) {
                save(path);
                ModLog.debug("配置文件补充/修正字段: enableCasinoRecipes=" + current.enableCasinoRecipes
                        + ", traumaHudConflictMode=" + current.traumaHudConflictMode
                        + ", overworldMaxBuildHeight=" + current.overworldMaxBuildHeight + ": " + path);
            } else {
                ModLog.debug("已加载模组配置: " + path + " (enableCasinoRecipes=" + current.enableCasinoRecipes
                        + ", traumaHudConflictMode=" + current.traumaHudConflictMode
                        + ", overworldMaxBuildHeight=" + current.overworldMaxBuildHeight + ")");
            }
        } catch (Exception e) {
            ModLog.error("[ModConfig] 读取配置文件失败，保持/回退默认配置: " + path + " -> " + e.getMessage(), e);
            current = new ConfigData();
        }
    }

    public static void save() {
        save(getConfigPath());
    }

    public static synchronized void save(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String json = GSON.toJson(current);
            lastSelfWriteNanos = System.nanoTime();
            lastKnownJsonContent = json;
            Files.writeString(path, json, StandardCharsets.UTF_8);
            ModLog.debug("已保存模组配置: " + path + " (enableCasinoRecipes=" + current.enableCasinoRecipes
                    + ", overworldMaxBuildHeight=" + current.overworldMaxBuildHeight + ")");
        } catch (Exception e) {
            ModLog.error("[ModConfig] 保存配置文件失败: " + path + " -> " + e.getMessage(), e);
        }
    }

    public static boolean isCasinoRecipesEnabled() {
        return current.enableCasinoRecipes;
    }

    public static void setCasinoRecipesEnabled(boolean enabled) {
        setCasinoRecipesEnabled(enabled, getConfigPath());
    }

    public static synchronized void setCasinoRecipesEnabled(boolean enabled, Path path) {
        current.enableCasinoRecipes = enabled;
        save(path);
        ModLog.debug("模组配置变更: enableCasinoRecipes=" + enabled);
    }

    public static TraumaHudConflictMode getTraumaHudConflictMode() {
        TraumaHudConflictMode mode = current.traumaHudConflictMode;
        return mode != null ? mode : TraumaHudConflictMode.ABOVE_CONFLICT;
    }

    public static void setTraumaHudConflictMode(TraumaHudConflictMode mode) {
        setTraumaHudConflictMode(mode, getConfigPath());
    }

    public static synchronized void setTraumaHudConflictMode(TraumaHudConflictMode mode, Path path) {
        current.traumaHudConflictMode = mode != null ? mode : TraumaHudConflictMode.ABOVE_CONFLICT;
        save(path);
        ModLog.debug("模组配置变更: traumaHudConflictMode=" + current.traumaHudConflictMode);
    }

    public static int getOverworldMaxBuildHeight() {
        return clampOverworldMaxBuildHeight(current.overworldMaxBuildHeight);
    }

    public static int getOverworldMaxPlaceableY() {
        return getOverworldMaxBuildHeight() - 1;
    }

    public static void setOverworldMaxBuildHeight(int height) {
        setOverworldMaxBuildHeight(height, getConfigPath());
    }

    public static synchronized void setOverworldMaxBuildHeight(int height, Path path) {
        int clamped = clampOverworldMaxBuildHeight(height);
        current.overworldMaxBuildHeight = clamped;
        save(path);
        ModLog.debug("模组配置变更: overworldMaxBuildHeight=" + clamped + " (maxY=" + (clamped - 1) + ")");
    }

    private static boolean hasAllCanvasFields(JsonObject obj) {
        return obj.has("canvasCreativeMode")
                && obj.has("allowPublicCanvas")
                && obj.has("allowCanvasCopying")
                && obj.has("maxCanvasesPerPlayer")
                && obj.has("maxPrintVoxels2D")
                && obj.has("maxPrintVoxels3D")
                && obj.has("voxelsPerPrintMaterial")
                && obj.has("printBaseMaterialCost")
                && obj.has("transparentVoxelCostMultiplier")
                && obj.has("emissiveVoxelCostMultiplier")
                && obj.has("printedModelLodDistance")
                && obj.has("printedModelMaxRenderDistance");
    }

    private static boolean sanitizeCanvasConfig(ConfigData data) {
        boolean changed = false;
        int maxPerPlayer = Math.max(1, Math.min(4096, data.maxCanvasesPerPlayer));
        if (maxPerPlayer != data.maxCanvasesPerPlayer) {
            data.maxCanvasesPerPlayer = maxPerPlayer;
            changed = true;
        }
        int max2d = Math.max(256, Math.min(65536, data.maxPrintVoxels2D));
        if (max2d != data.maxPrintVoxels2D) {
            data.maxPrintVoxels2D = max2d;
            changed = true;
        }
        int max3d = Math.max(4096, Math.min(2097152, data.maxPrintVoxels3D));
        if (max3d != data.maxPrintVoxels3D) {
            data.maxPrintVoxels3D = max3d;
            changed = true;
        }
        int perMat = Math.max(1, Math.min(65536, data.voxelsPerPrintMaterial));
        if (perMat != data.voxelsPerPrintMaterial) {
            data.voxelsPerPrintMaterial = perMat;
            changed = true;
        }
        int baseCost = Math.max(0, Math.min(1024, data.printBaseMaterialCost));
        if (baseCost != data.printBaseMaterialCost) {
            data.printBaseMaterialCost = baseCost;
            changed = true;
        }
        double transMult = Math.max(0.1, Math.min(10.0, data.transparentVoxelCostMultiplier));
        if (Double.compare(transMult, data.transparentVoxelCostMultiplier) != 0) {
            data.transparentVoxelCostMultiplier = transMult;
            changed = true;
        }
        double emissiveMult = Math.max(0.1, Math.min(10.0, data.emissiveVoxelCostMultiplier));
        if (Double.compare(emissiveMult, data.emissiveVoxelCostMultiplier) != 0) {
            data.emissiveVoxelCostMultiplier = emissiveMult;
            changed = true;
        }
        int lodDist = Math.max(8, Math.min(256, data.printedModelLodDistance));
        if (lodDist != data.printedModelLodDistance) {
            data.printedModelLodDistance = lodDist;
            changed = true;
        }
        int maxRenderDist = Math.max(lodDist, Math.min(512, data.printedModelMaxRenderDistance));
        if (maxRenderDist != data.printedModelMaxRenderDistance) {
            data.printedModelMaxRenderDistance = maxRenderDist;
            changed = true;
        }
        return changed;
    }

    public static boolean isCanvasCreativeMode() {
        return current.canvasCreativeMode;
    }

    public static synchronized void setCanvasCreativeMode(boolean enabled, Path path) {
        current.canvasCreativeMode = enabled;
        save(path);
        ModLog.debug("模组配置变更: canvasCreativeMode=" + enabled);
    }

    public static void setCanvasCreativeMode(boolean enabled) {
        setCanvasCreativeMode(enabled, getConfigPath());
    }

    public static boolean isAllowPublicCanvas() {
        return current.allowPublicCanvas;
    }

    public static synchronized void setAllowPublicCanvas(boolean allowed, Path path) {
        current.allowPublicCanvas = allowed;
        save(path);
        ModLog.debug("模组配置变更: allowPublicCanvas=" + allowed);
    }

    public static void setAllowPublicCanvas(boolean allowed) {
        setAllowPublicCanvas(allowed, getConfigPath());
    }

    public static boolean isAllowCanvasCopying() {
        return current.allowCanvasCopying;
    }

    public static synchronized void setAllowCanvasCopying(boolean allowed, Path path) {
        current.allowCanvasCopying = allowed;
        save(path);
        ModLog.debug("模组配置变更: allowCanvasCopying=" + allowed);
    }

    public static void setAllowCanvasCopying(boolean allowed) {
        setAllowCanvasCopying(allowed, getConfigPath());
    }

    public static int getMaxCanvasesPerPlayer() {
        return current.maxCanvasesPerPlayer;
    }

    public static int getMaxPrintVoxels2D() {
        return current.maxPrintVoxels2D;
    }

    public static int getMaxPrintVoxels3D() {
        return current.maxPrintVoxels3D;
    }

    public static int getVoxelsPerPrintMaterial() {
        return current.voxelsPerPrintMaterial;
    }

    public static int getPrintBaseMaterialCost() {
        return current.printBaseMaterialCost;
    }

    public static double getTransparentVoxelCostMultiplier() {
        return current.transparentVoxelCostMultiplier;
    }

    public static double getEmissiveVoxelCostMultiplier() {
        return current.emissiveVoxelCostMultiplier;
    }

    public static int getPrintedModelLodDistance() {
        return current.printedModelLodDistance;
    }

    public static int getPrintedModelMaxRenderDistance() {
        return current.printedModelMaxRenderDistance;
    }

    public enum TraumaHudConflictMode {
        ABOVE_CONFLICT,
        RIGHT_CENTER
    }

    public static final class ConfigData {
        public boolean enableCasinoRecipes = true;
        public TraumaHudConflictMode traumaHudConflictMode = TraumaHudConflictMode.ABOVE_CONFLICT;
        public int overworldMaxBuildHeight = DEFAULT_OVERWORLD_MAX_BUILD_HEIGHT;
        public boolean canvasCreativeMode = false;
        public boolean allowPublicCanvas = true;
        public boolean allowCanvasCopying = true;
        public int maxCanvasesPerPlayer = 64;
        public int maxPrintVoxels2D = 65536;
        public int maxPrintVoxels3D = 2097152;
        public int voxelsPerPrintMaterial = 256;
        public int printBaseMaterialCost = 0;
        public double transparentVoxelCostMultiplier = 1.0;
        public double emissiveVoxelCostMultiplier = 1.0;
        public int printedModelLodDistance = 48;
        public int printedModelMaxRenderDistance = 96;
    }
}
