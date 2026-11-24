package me.tuanzi.block.entity;

import me.tuanzi.util.ModLog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 活塞推动方块破坏器状态迁移管理器
 * 确保在活塞移动期间方块破坏器的全部物品、设置与状态无损迁移，不掉落、不重复、不误触发。
 */
public class BlockBreakerPistonManager {
    private static final Map<GlobalPos, CompoundTag> PENDING_MIGRATIONS = new ConcurrentHashMap<>();

    public static void recordMigration(ResourceKey<Level> dimension, BlockPos targetPos, CompoundTag stateTag) {
        GlobalPos key = GlobalPos.of(dimension, targetPos.immutable());
        PENDING_MIGRATIONS.put(key, stateTag);
        ModLog.debug("[方块破坏器活塞迁移] 记录目标位置暂存数据: " + key + ", tag=" + stateTag);
    }

    public static @Nullable CompoundTag getMigration(ResourceKey<Level> dimension, BlockPos currentPos) {
        GlobalPos key = GlobalPos.of(dimension, currentPos.immutable());
        return PENDING_MIGRATIONS.get(key);
    }

    public static @Nullable CompoundTag consumeMigration(ResourceKey<Level> dimension, BlockPos currentPos) {
        GlobalPos key = GlobalPos.of(dimension, currentPos.immutable());
        CompoundTag removed = PENDING_MIGRATIONS.remove(key);
        if (removed != null) {
            ModLog.debug("[方块破坏器活塞迁移] 消费并清理暂存数据: " + key);
        }
        return removed;
    }

    public static void confirmMigration(ResourceKey<Level> dimension, BlockPos currentPos) {
        GlobalPos key = GlobalPos.of(dimension, currentPos.immutable());
        CompoundTag removed = PENDING_MIGRATIONS.remove(key);
        if (removed != null) {
            ModLog.debug("[方块破坏器活塞迁移] 确认完成并清理暂存数据: " + key);
        }
    }

    public static boolean hasPendingMigration(ResourceKey<Level> dimension, BlockPos currentPos) {
        return PENDING_MIGRATIONS.containsKey(GlobalPos.of(dimension, currentPos.immutable()));
    }

    public static void clear() {
        PENDING_MIGRATIONS.clear();
    }
}
