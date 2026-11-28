package me.tuanzi.integration.voxy;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.tuanzi.block.entity.ColorBlockBlockEntity;
import me.tuanzi.block.entity.ColorSlabBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * 单个 16x16x16 区块段内的彩色方块颜色快照（不可变用于后台线程读取）。
 * 局部索引编码为 {@code ((ly & 0xF) << 8) | ((lz & 0xF) << 4) | (lx & 0xF)}，与 Voxy {@code WorldConversionFactory} 保持一致。
 */
public final class VoxyColorSectionSnapshot {
    private static final String COLOR_BLOCK_BE_ID = "tuanzis_mod:color_block_be";
    private static final String COLOR_SLAB_BE_ID = "tuanzis_mod:color_slab_be";

    private final Int2LongOpenHashMap localColors;

    public VoxyColorSectionSnapshot() {
        this.localColors = new Int2LongOpenHashMap();
        this.localColors.defaultReturnValue(-1L);
    }

    public static int toLocalIndex(int lx, int ly, int lz) {
        return ((ly & 0xF) << 8) | ((lz & 0xF) << 4) | (lx & 0xF);
    }

    public void put(int lx, int ly, int lz, int primaryColor, int topColor) {
        this.localColors.put(toLocalIndex(lx, ly, lz), VoxyColorSupport.packColorPair(primaryColor, topColor));
    }

    public boolean isEmpty() {
        return this.localColors.isEmpty();
    }

    public int size() {
        return this.localColors.size();
    }

    public long getPacked(int localIndex) {
        return this.localColors.get(localIndex);
    }

    public Iterable<Int2LongMap.Entry> entries() {
        return this.localColors.int2LongEntrySet();
    }

    /**
     * 从已加载区块的方块实体映射中提取按 sectionY 分组的颜色快照；若无彩色方块实体返回 null。
     */
    public static @Nullable Int2ObjectMap<VoxyColorSectionSnapshot> fromChunkBlockEntities(@Nullable LevelChunk chunk) {
        if (chunk == null) {
            return null;
        }
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
        if (blockEntities == null || blockEntities.isEmpty()) {
            return null;
        }

        Int2ObjectOpenHashMap<VoxyColorSectionSnapshot> bySection = null;
        for (BlockEntity be : blockEntities.values()) {
            if (be instanceof ColorBlockBlockEntity colorBe) {
                BlockPos pos = colorBe.getBlockPos();
                if (bySection == null) {
                    bySection = new Int2ObjectOpenHashMap<>();
                }
                bySection.computeIfAbsent(pos.getY() >> 4, k -> new VoxyColorSectionSnapshot())
                        .put(pos.getX(), pos.getY(), pos.getZ(), colorBe.getColor() & 0xFFFFFF, VoxyColorSupport.NO_COLOR);
            } else if (be instanceof ColorSlabBlockEntity slabBe) {
                BlockPos pos = slabBe.getBlockPos();
                if (bySection == null) {
                    bySection = new Int2ObjectOpenHashMap<>();
                }
                bySection.computeIfAbsent(pos.getY() >> 4, k -> new VoxyColorSectionSnapshot())
                        .put(pos.getX(), pos.getY(), pos.getZ(), slabBe.getBottomColor() & 0xFFFFFF, slabBe.getTopColor() & 0xFFFFFF);
            }
        }
        return bySection;
    }

    /**
     * 仅提取指定区块段（sectionY）内的彩色方块实体颜色快照；若无匹配返回 null。
     */
    public static @Nullable VoxyColorSectionSnapshot fromChunkSection(@Nullable LevelChunk chunk, int sectionY) {
        if (chunk == null) {
            return null;
        }
        Map<BlockPos, BlockEntity> blockEntities = chunk.getBlockEntities();
        if (blockEntities == null || blockEntities.isEmpty()) {
            return null;
        }

        VoxyColorSectionSnapshot snapshot = null;
        for (BlockEntity be : blockEntities.values()) {
            BlockPos pos = be.getBlockPos();
            if ((pos.getY() >> 4) != sectionY) {
                continue;
            }
            if (be instanceof ColorBlockBlockEntity colorBe) {
                if (snapshot == null) {
                    snapshot = new VoxyColorSectionSnapshot();
                }
                snapshot.put(pos.getX(), pos.getY(), pos.getZ(), colorBe.getColor() & 0xFFFFFF, VoxyColorSupport.NO_COLOR);
            } else if (be instanceof ColorSlabBlockEntity slabBe) {
                if (snapshot == null) {
                    snapshot = new VoxyColorSectionSnapshot();
                }
                snapshot.put(pos.getX(), pos.getY(), pos.getZ(), slabBe.getBottomColor() & 0xFFFFFF, slabBe.getTopColor() & 0xFFFFFF);
            }
        }
        return snapshot;
    }

    /**
     * 从区块离线 NBT（.mca 区域文件）的 {@code block_entities} 列表中提取按 sectionY 分组的颜色快照。
     */
    public static @Nullable Int2ObjectMap<VoxyColorSectionSnapshot> fromChunkNbtBlockEntities(@Nullable CompoundTag chunkTag) {
        if (chunkTag == null || !chunkTag.contains("block_entities")) {
            return null;
        }
        Optional<ListTag> optList = chunkTag.getList("block_entities");
        if (optList.isEmpty() || optList.get().isEmpty()) {
            return null;
        }

        Int2ObjectOpenHashMap<VoxyColorSectionSnapshot> bySection = null;
        for (Tag tag : optList.get()) {
            if (!(tag instanceof CompoundTag beTag)) {
                continue;
            }
            String id = beTag.getStringOr("id", "");
            if (COLOR_BLOCK_BE_ID.equals(id)) {
                int x = beTag.getIntOr("x", 0);
                int y = beTag.getIntOr("y", 0);
                int z = beTag.getIntOr("z", 0);
                int color = beTag.getIntOr("Color", 0xFFFFFF) & 0xFFFFFF;
                if (bySection == null) {
                    bySection = new Int2ObjectOpenHashMap<>();
                }
                bySection.computeIfAbsent(y >> 4, k -> new VoxyColorSectionSnapshot())
                        .put(x, y, z, color, VoxyColorSupport.NO_COLOR);
            } else if (COLOR_SLAB_BE_ID.equals(id)) {
                int x = beTag.getIntOr("x", 0);
                int y = beTag.getIntOr("y", 0);
                int z = beTag.getIntOr("z", 0);
                int bottom = beTag.getIntOr("BottomColor", 0xFFFFFF) & 0xFFFFFF;
                int top = beTag.getIntOr("TopColor", 0xFFFFFF) & 0xFFFFFF;
                if (bySection == null) {
                    bySection = new Int2ObjectOpenHashMap<>();
                }
                bySection.computeIfAbsent(y >> 4, k -> new VoxyColorSectionSnapshot())
                        .put(x, y, z, bottom, top);
            }
        }
        return bySection;
    }
}
