package me.tuanzi.integration.voxy;

import me.tuanzi.block.ColorBlock;
import me.tuanzi.block.ColorSlab;
import me.tuanzi.block.ColorStairs;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * Voxy 彩色方块系列 LOD 颜色支持工具类。
 * 本类刻意不直接引用任何 Voxy 类，确保在未安装 Voxy 的运行环境下可安全加载与进行单元测试。
 */
public final class VoxyColorSupport {
    /**
     * 未降级量化前允许保留的精确 24-bit 彩色状态条目上限，避免撑爆 Voxy 65536 模型槽位。
     */
    public static final int MAX_EXACT_COLOR_ENTRIES = 4096;
    public static final int NO_COLOR = -1;

    @FunctionalInterface
    public interface SectionSnapshotExtractor {
        @Nullable
        VoxyColorSectionSnapshot extract(int sectionX, int sectionY, int sectionZ);
    }

    private static volatile @Nullable BiConsumer<Level, BlockPos> clientDirtyNotifier;
    private static volatile @Nullable SectionSnapshotExtractor sectionSnapshotExtractor;

    private VoxyColorSupport() {
    }

    /**
     * 由客户端 Voxy 桥接模块注册实时变更通知与区块段快照提取回调。
     */
    public static void registerClientHooks(
            BiConsumer<Level, BlockPos> dirtyNotifier,
            SectionSnapshotExtractor snapshotExtractor
    ) {
        clientDirtyNotifier = dirtyNotifier;
        sectionSnapshotExtractor = snapshotExtractor;
    }

    /**
     * 通知 Voxy 客户端桥接层某个彩色方块位置颜色已更新。
     */
    public static void notifyClientColorChanged(@Nullable Level level, @Nullable BlockPos pos) {
        if (level == null || pos == null || !level.isClientSide()) {
            return;
        }
        BiConsumer<Level, BlockPos> notifier = clientDirtyNotifier;
        if (notifier != null) {
            notifier.accept(level, pos);
        }
    }

    /**
     * 提取客户端指定区块段内的彩色方块实体颜色快照。
     */
    public static @Nullable VoxyColorSectionSnapshot extractSectionSnapshot(int sectionX, int sectionY, int sectionZ) {
        SectionSnapshotExtractor extractor = sectionSnapshotExtractor;
        return extractor != null ? extractor.extract(sectionX, sectionY, sectionZ) : null;
    }

    /**
     * 判断方块状态是否属于彩色方块系列（彩色方块、彩色半砖、彩色楼梯）。
     */
    public static boolean isColorBlockState(@Nullable BlockState state) {
        if (state == null) {
            return false;
        }
        var block = state.getBlock();
        return block instanceof ColorBlock || block instanceof ColorSlab || block instanceof ColorStairs;
    }

    /**
     * 快速检测区块段中是否可能存在彩色方块，避免无谓的方块实体扫描。
     */
    public static boolean sectionMayContainColorBlocks(@Nullable LevelChunkSection section) {
        return section != null && !section.hasOnlyAir() && section.maybeHas(VoxyColorSupport::isColorBlockState);
    }

    /**
     * 将 24-bit RGB 颜色量化到 RGB444（每通道 4 bit，共最多 4096 色），用于状态数超过上限时兜底。
     */
    public static int quantizeRgb444(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int r4 = (r * 15 + 127) / 255;
        int g4 = (g * 15 + 127) / 255;
        int b4 = (b * 15 + 127) / 255;
        return ((r4 * 17) << 16) | ((g4 * 17) << 8) | (b4 * 17);
    }

    /**
     * 规范化方块状态对应的主色与双色顶色。
     * <ul>
     *   <li>单色方块/楼梯/半砖（BOTTOM/TOP/同色 DOUBLE）：仅保留主色，顶色为 {@link #NO_COLOR}</li>
     *   <li>异色双半砖（DOUBLE 且上下颜色不同）：主色为底部颜色，顶色为顶部颜色</li>
     * </ul>
     */
    public static long normalizeColors(BlockState state, int primaryColor, int secondaryTopColor) {
        int c0 = primaryColor & 0xFFFFFF;
        if (state.getBlock() instanceof ColorSlab) {
            int c1 = secondaryTopColor >= 0 ? (secondaryTopColor & 0xFFFFFF) : c0;
            SlabType type = state.getValue(SlabBlock.TYPE);
            if (type == SlabType.BOTTOM) {
                return packColorPair(c0, NO_COLOR);
            }
            if (type == SlabType.TOP) {
                return packColorPair(c1, NO_COLOR);
            }
            // DOUBLE
            return c0 == c1 ? packColorPair(c0, NO_COLOR) : packColorPair(c0, c1);
        }
        return packColorPair(c0, NO_COLOR);
    }

    public static long packColorPair(int primaryColor, int topColor) {
        return (((long) topColor) << 32) | (primaryColor & 0xFFFFFFFFL);
    }

    public static int unpackPrimary(long packed) {
        return (int) packed;
    }

    public static int unpackTop(long packed) {
        return (int) (packed >>> 32);
    }

    /**
     * 按 ABGR 像素格式将单个采样像素乘以 24-bit RGB 颜色，保留原始 Alpha 通道。
     */
    public static int multiplyAbgrPixelByRgb(int abgr, int rgb) {
        int a = abgr & 0xFF000000;
        int pr = abgr & 0xFF;
        int pg = (abgr >> 8) & 0xFF;
        int pb = (abgr >> 16) & 0xFF;

        int tr = (rgb >> 16) & 0xFF;
        int tg = (rgb >> 8) & 0xFF;
        int tb = rgb & 0xFF;

        int nr = (pr * tr + 127) / 255;
        int ng = (pg * tg + 127) / 255;
        int nb = (pb * tb + 127) / 255;
        return a | (nb << 16) | (ng << 8) | nr;
    }

    /**
     * 为异色双半砖的单个面生成预乘颜色后的 ABGR 像素数组。
     * 依据 Voxy {@code SoftwareModelTextureBakery.VIEWS} 的投影映射：
     * <ul>
     *   <li>face 0 (DOWN)：整面使用 bottomRgb</li>
     *   <li>face 1 (UP)：整面使用 topRgb</li>
     *   <li>face 2 (NORTH) / 3 (SOUTH)：py &lt; height/2 使用 bottomRgb，其余使用 topRgb</li>
     *   <li>face 4 (WEST) / 5 (EAST)：px &lt; width/2 使用 bottomRgb，其余使用 topRgb</li>
     * </ul>
     */
    public static int[] applyDualSlabColorsToFace(int face, int[] srcAbgr, int width, int height, int bottomRgb, int topRgb) {
        int[] out = new int[srcAbgr.length];
        int halfH = height / 2;
        int halfW = width / 2;
        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int idx = px + py * width;
                int targetRgb;
                if (face == 0) {
                    targetRgb = bottomRgb;
                } else if (face == 1) {
                    targetRgb = topRgb;
                } else if (face == 2 || face == 3) {
                    targetRgb = (py < halfH) ? bottomRgb : topRgb;
                } else if (face == 4 || face == 5) {
                    targetRgb = (px < halfW) ? bottomRgb : topRgb;
                } else {
                    targetRgb = bottomRgb;
                }
                out[idx] = multiplyAbgrPixelByRgb(srcAbgr[idx], targetRgb);
            }
        }
        return out;
    }
}
