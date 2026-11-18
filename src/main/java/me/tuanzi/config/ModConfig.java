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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile ConfigData current = new ConfigData();

    private ModConfig() {
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
                        + " (enableCasinoRecipes=" + current.enableCasinoRecipes + ")");
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
            current = loaded;

            if (!obj.has("enableCasinoRecipes")) {
                save(path);
                ModLog.debug("配置文件补充缺失字段 enableCasinoRecipes=" + current.enableCasinoRecipes + ": " + path);
            } else {
                ModLog.debug("已加载模组配置: " + path + " (enableCasinoRecipes=" + current.enableCasinoRecipes + ")");
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
            Files.writeString(path, GSON.toJson(current), StandardCharsets.UTF_8);
            ModLog.debug("已保存模组配置: " + path + " (enableCasinoRecipes=" + current.enableCasinoRecipes + ")");
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

    public static final class ConfigData {
        public boolean enableCasinoRecipes = true;
    }
}
