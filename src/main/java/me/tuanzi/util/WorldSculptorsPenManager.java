package me.tuanzi.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WorldSculptorsPenManager {

    public static class FillRecord {
        public final UUID recordId;
        public final ResourceKey<Level> dimension;
        public final List<BlockStateRecord> blockRecords;
        public final long timestamp; // 毫秒数
        public final String blockId; // 填充的方块注册ID
        public final int fillCount;  // 填充的方块数

        public FillRecord(UUID recordId, ResourceKey<Level> dimension, List<BlockStateRecord> blockRecords, String blockId, int fillCount) {
            this.recordId = recordId;
            this.dimension = dimension;
            this.blockRecords = blockRecords;
            this.timestamp = System.currentTimeMillis();
            this.blockId = blockId;
            this.fillCount = fillCount;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - this.timestamp > 60000; // 60秒过期
        }
    }

    public static class BlockStateRecord {
        public final BlockPos pos;
        public final BlockState oldState;

        public BlockStateRecord(BlockPos pos, BlockState oldState) {
            this.pos = pos;
            this.oldState = oldState;
        }
    }

    private static final Map<UUID, FillRecord> RECORDS = new ConcurrentHashMap<>();

    /**
     * 新增一次填充记录
     */
    public static void addRecord(UUID id, ResourceKey<Level> dimension, List<BlockStateRecord> records, String blockId, int count) {
        RECORDS.put(id, new FillRecord(id, dimension, records, blockId, count));
        cleanExpiredRecords();
    }

    /**
     * 获取一次填充记录，如果过期则移除并返回null
     */
    public static FillRecord getRecord(UUID id) {
        if (id == null) return null;
        FillRecord record = RECORDS.get(id);
        if (record == null) return null;
        if (record.isExpired()) {
            RECORDS.remove(id);
            return null;
        }
        return record;
    }

    /**
     * 显式移除填充记录
     */
    public static void removeRecord(UUID id) {
        RECORDS.remove(id);
    }

    /**
     * 清理所有过期记录
     */
    public static void cleanExpiredRecords() {
        long now = System.currentTimeMillis();
        RECORDS.entrySet().removeIf(entry -> now - entry.getValue().timestamp > 60000);
    }
}
