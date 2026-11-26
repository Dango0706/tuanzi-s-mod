package me.tuanzi.client.renderer.blueprint;

import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import me.tuanzi.block.entity.ColorBlockBlockEntity;
import me.tuanzi.block.entity.ColorSlabBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 蓝图 3D 预览用的轻量只读 BlockAndTintGetter：
 * 1) 按切层可见性裁剪范围，使切层顶面/底面处邻居返回 AIR 从而正确暴露断面；
 * 2) 为水、草地、树叶提供原版标准生物群系染色（修复水显示为淡灰色的问题）；
 * 3) 为彩色方块/半砖/楼梯按 BlockEntities NBT 提供只读预览方块实体，支持全真原色染色预览；
 * 4) 提供满亮度 (15) 与默认六向光照，保证立体感与细节清晰度。
 */
@Environment(EnvType.CLIENT)
public class BlueprintPreviewLevel implements BlockAndTintGetter {

    public static final int DEFAULT_WATER_COLOR = 0xFF3F76E4;
    public static final int DEFAULT_GRASS_COLOR = 0xFF79C05A;
    public static final int DEFAULT_DRY_FOLIAGE_COLOR = 0xFFA37B46;

    private final int width;
    private final int height;
    private final int length;
    private final BlockState[] palette;
    private final int[] blocks;
    private final Map<BlockPos, CompoundTag> teNbtMap;
    private final int currentLayer;
    private final boolean singleLayerMode;
    private int tintedBlockLookupCount = 0;

    public BlueprintPreviewLevel(
            int width,
            int height,
            int length,
            BlockState[] palette,
            int[] blocks,
            int currentLayer,
            boolean singleLayerMode
    ) {
        this(width, height, length, palette, blocks, Map.of(), currentLayer, singleLayerMode);
    }

    public BlueprintPreviewLevel(
            int width,
            int height,
            int length,
            BlockState[] palette,
            int[] blocks,
            @Nullable Map<BlockPos, CompoundTag> teNbtMap,
            int currentLayer,
            boolean singleLayerMode
    ) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.palette = palette;
        this.blocks = blocks;
        this.teNbtMap = teNbtMap != null ? teNbtMap : Map.of();
        this.currentLayer = currentLayer;
        this.singleLayerMode = singleLayerMode;
    }

    public int getTintedBlockLookupCount() {
        return this.tintedBlockLookupCount;
    }

    public boolean isLayerVisible(int y) {
        if (y < 0 || y >= height) {
            return false;
        }
        return singleLayerMode ? (y == currentLayer) : (y <= currentLayer);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (x < 0 || x >= width || z < 0 || z >= length || !isLayerVisible(y)) {
            return Blocks.AIR.defaultBlockState();
        }
        long idxLong = (long) y * (long) width * (long) length + (long) z * (long) width + (long) x;
        if (idxLong < 0 || idxLong >= blocks.length) {
            return Blocks.AIR.defaultBlockState();
        }
        int idx = (int) idxLong;
        int pIdx = blocks[idx];
        if (pIdx < 0 || pIdx >= palette.length) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState state = palette[pIdx];
        if (state == null || state.is(Blocks.BARRIER) || state.is(Blocks.STRUCTURE_VOID)) {
            return Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public CardinalLighting cardinalLighting() {
        return CardinalLighting.DEFAULT;
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return LevelLightEngine.EMPTY;
    }

    @Override
    public int getBrightness(LightLayer layer, BlockPos pos) {
        return 15;
    }

    @Override
    public int getRawBrightness(BlockPos pos, int darkening) {
        return 15;
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return true;
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver color) {
        if (color == BiomeColors.WATER_COLOR_RESOLVER) {
            return DEFAULT_WATER_COLOR;
        }
        if (color == BiomeColors.GRASS_COLOR_RESOLVER) {
            int grass = GrassColor.getDefaultColor();
            return (grass & 0x00FFFFFF) == 0 ? DEFAULT_GRASS_COLOR : ARGB.opaque(grass);
        }
        if (color == BiomeColors.FOLIAGE_COLOR_RESOLVER) {
            return ARGB.opaque(FoliageColor.FOLIAGE_DEFAULT);
        }
        if (color == BiomeColors.DRY_FOLIAGE_COLOR_RESOLVER) {
            return DEFAULT_DRY_FOLIAGE_COLOR;
        }
        return -1;
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        if (pos == null) {
            return null;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (x < 0 || x >= width || z < 0 || z >= length || !isLayerVisible(y)) {
            return null;
        }
        BlockState state = getBlockState(pos);
        Block block = state.getBlock();
        if (block instanceof ColorBlock || block instanceof ColorStairs) {
            ColorBlockBlockEntity be = new ColorBlockBlockEntity(pos.immutable(), state);
            CompoundTag nbt = teNbtMap.get(pos);
            if (nbt != null) {
                be.setPreviewColor(nbt.getIntOr("Color", 0xFFFFFF));
            }
            tintedBlockLookupCount++;
            return be;
        }
        if (block instanceof ColorSlab) {
            ColorSlabBlockEntity be = new ColorSlabBlockEntity(pos.immutable(), state);
            CompoundTag nbt = teNbtMap.get(pos);
            if (nbt != null) {
                be.setPreviewColors(
                        nbt.getIntOr("BottomColor", 0xFFFFFF),
                        nbt.getIntOr("TopColor", 0xFFFFFF),
                        nbt.getBooleanOr("BottomSet", false),
                        nbt.getBooleanOr("TopSet", false)
                );
            }
            tintedBlockLookupCount++;
            return be;
        }
        return null;
    }

    @Override
    public int getHeight() {
        return Math.max(1, height);
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
