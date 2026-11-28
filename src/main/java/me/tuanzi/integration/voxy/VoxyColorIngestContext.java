package me.tuanzi.integration.voxy;

import org.jetbrains.annotations.Nullable;

/**
 * 线程局部的 Voxy 区块段彩色快照上下文，用于在 {@code VoxelIngestService} / {@code WorldImporter}
 * 与 {@code WorldConversionFactory#convert} 之间传递当前处理的彩色方块实体数据。
 */
public final class VoxyColorIngestContext {
    private static final ThreadLocal<VoxyColorSectionSnapshot> CURRENT_SNAPSHOT = new ThreadLocal<>();

    private VoxyColorIngestContext() {
    }

    public static void setCurrent(@Nullable VoxyColorSectionSnapshot snapshot) {
        if (snapshot == null || snapshot.isEmpty()) {
            CURRENT_SNAPSHOT.remove();
        } else {
            CURRENT_SNAPSHOT.set(snapshot);
        }
    }

    public static @Nullable VoxyColorSectionSnapshot getCurrent() {
        return CURRENT_SNAPSHOT.get();
    }

    public static void clear() {
        CURRENT_SNAPSHOT.remove();
    }
}
