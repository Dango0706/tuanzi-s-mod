package me.tuanzi.world.canvas;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.BiFunction;

/**
 * 服务端/客户端通用桥接：允许 PrintedModelBlockEntity 在客户端安全查询已同步的体素碰撞数据而不直接依赖客户端类。
 */
public final class CanvasClientCollisionBridge {
    private static volatile @Nullable BiFunction<UUID, String, CanvasVoxelData> clientVoxelLookup = null;
    private static volatile @Nullable BiFunction<UUID, String, Boolean> clientRequestTrigger = null;

    private CanvasClientCollisionBridge() {
    }

    public static void registerClientHandlers(
            BiFunction<UUID, String, CanvasVoxelData> lookup,
            BiFunction<UUID, String, Boolean> requestTrigger
    ) {
        clientVoxelLookup = lookup;
        clientRequestTrigger = requestTrigger;
    }

    public static @Nullable CanvasVoxelData getClientVoxelData(UUID uuid, String hash) {
        BiFunction<UUID, String, CanvasVoxelData> fn = clientVoxelLookup;
        return fn != null ? fn.apply(uuid, hash) : null;
    }

    public static void requestClientDataIfMissing(UUID uuid, String hash) {
        BiFunction<UUID, String, Boolean> fn = clientRequestTrigger;
        if (fn != null) {
            fn.apply(uuid, hash);
        }
    }
}
