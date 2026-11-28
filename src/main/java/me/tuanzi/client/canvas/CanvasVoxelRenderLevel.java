package me.tuanzi.client.canvas;

import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import me.tuanzi.block.entity.ColorBlockBlockEntity;
import me.tuanzi.block.entity.ColorSlabBlockEntity;
import me.tuanzi.world.canvas.CanvasPaletteEntry;
import me.tuanzi.world.canvas.CanvasVoxelData;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
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

import java.util.List;

/**
 * 供打印模型静态网格烘焙使用的只读 BlockAndTintGetter：
 * 精确保留原版流体/植被群系染色及本模组 ColorBlock/ColorSlab/ColorStairs 自定义 RGB 色彩。
 */
@Environment(EnvType.CLIENT)
public class CanvasVoxelRenderLevel implements BlockAndTintGetter {
    private static final int DEFAULT_WATER_COLOR = 0xFF3F76E4;
    private static final int DEFAULT_GRASS_COLOR = 0xFF79C05A;
    private static final int DEFAULT_DRY_FOLIAGE_COLOR = 0xFFA37B46;

    private final CanvasVoxelData data;
    private final BlockState[] resolvedPalette;
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;

    public CanvasVoxelRenderLevel(CanvasVoxelData data, BlockState[] resolvedPalette) {
        this.data = data;
        this.resolvedPalette = resolvedPalette;
        this.sizeX = data.getSizeX();
        this.sizeY = data.getSizeY();
        this.sizeZ = data.getSizeZ();
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
            return Blocks.AIR.defaultBlockState();
        }
        int pIdx = data.getPaletteIndex(x, y, z);
        if (pIdx < 0 || pIdx >= resolvedPalette.length) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState s = resolvedPalette[pIdx];
        return s != null ? s : Blocks.AIR.defaultBlockState();
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
        int pIdx = data.getPaletteIndex(pos.getX(), pos.getY(), pos.getZ());
        List<CanvasPaletteEntry> entries = data.getPalette();
        if (pIdx < 0 || pIdx >= entries.size()) {
            return null;
        }
        BlockState state = resolvedPalette[pIdx];
        Block block = state.getBlock();
        CanvasPaletteEntry entry = entries.get(pIdx);
        if (block instanceof ColorBlock || block instanceof ColorStairs) {
            ColorBlockBlockEntity be = new ColorBlockBlockEntity(pos.immutable(), state);
            if (entry.colorRgb() != CanvasPaletteEntry.NO_COLOR) {
                be.setPreviewColor(entry.colorRgb());
            }
            return be;
        }
        if (block instanceof ColorSlab) {
            ColorSlabBlockEntity be = new ColorSlabBlockEntity(pos.immutable(), state);
            int bot = entry.colorRgb() != CanvasPaletteEntry.NO_COLOR ? entry.colorRgb() : 0xFFFFFF;
            int top = entry.altColorRgb() != CanvasPaletteEntry.NO_COLOR ? entry.altColorRgb() : bot;
            be.setPreviewColors(bot, top, true, true);
            return be;
        }
        return null;
    }

    @Override
    public int getHeight() {
        return Math.max(1, sizeY);
    }

    @Override
    public int getMinY() {
        return 0;
    }
}
