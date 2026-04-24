package me.tuanzi.util;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModLog {
    private static final Logger LOGGER = LoggerFactory.getLogger("TuanzisMod");
    private static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();

    /**
     * 调试日志
     * 开发环境：输出到控制台（通过 [DEBUG] 标识）
     * 生产环境：仅输出到日志文件（使用标准 DEBUG 级别，默认不显示在控制台）
     */
    public static void debug(String message) {
        if (IS_DEV) {
            // 开发环境下，使用 INFO 级别确保在控制台直接可见
            LOGGER.info("[DEBUG] " + message);
        } else {
            // 生产环境下，使用 DEBUG 级别。
            // 默认 Log4j 配置会将 DEBUG 写入日志文件，但不会在控制台打印。
            LOGGER.debug(message);
        }
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }

    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
}
