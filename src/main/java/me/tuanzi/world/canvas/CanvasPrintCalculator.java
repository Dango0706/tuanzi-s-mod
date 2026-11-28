package me.tuanzi.world.canvas;

import me.tuanzi.config.ModConfig;
import me.tuanzi.util.ModLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D 打印机与复印机消耗、缩放倍率及体素上限纯计算工具类。
 */
public final class CanvasPrintCalculator {
    private CanvasPrintCalculator() {
    }

    public record PrintCostReport(
            int nonEmptyVoxels,
            int transparentVoxels,
            int emissiveVoxels,
            int voxelLimit,
            boolean exceedsLimit,
            int scaleMultiplier,
            double weightedVoxels,
            int materialCost
    ) {
    }

    /**
     * 返回指定画布尺寸与类型在 3D 打印机中允许选择的放置格数倍率（保持每格至少 16 体素密度）。
     */
    public static List<Integer> getAllowedScales(int canvasSize, CanvasType type) {
        int maxScaleByDensity = Math.max(1, canvasSize / 16);
        int hardMax = (type == CanvasType.VOXEL_3D) ? 8 : 16;
        int cap = Math.min(maxScaleByDensity, hardMax);
        List<Integer> scales = new ArrayList<>();
        for (int s = 1; s <= cap; s *= 2) {
            scales.add(s);
        }
        return scales;
    }

    public static int clampScale(int requestedScale, int canvasSize, CanvasType type) {
        List<Integer> allowed = getAllowedScales(canvasSize, type);
        if (allowed.contains(requestedScale)) {
            return requestedScale;
        }
        return allowed.isEmpty() ? 1 : allowed.getFirst();
    }

    public static int getVoxelLimitForType(CanvasType type) {
        return (type == CanvasType.VOXEL_3D)
                ? ModConfig.getMaxPrintVoxels3D()
                : ModConfig.getMaxPrintVoxels2D();
    }

    public static PrintCostReport calculate(CanvasVoxelData data, int requestedScale) {
        var counts = data.classifyVoxels();
        int scale = clampScale(requestedScale, Math.max(data.getSizeX(), data.getSizeZ()), data.getType());
        int limit = getVoxelLimitForType(data.getType());
        boolean exceeds = counts.totalNonEmpty() > limit;

        double transMult = ModConfig.getTransparentVoxelCostMultiplier();
        double emissiveMult = ModConfig.getEmissiveVoxelCostMultiplier();
        int voxelsPerMat = Math.max(1, ModConfig.getVoxelsPerPrintMaterial());
        int baseCost = Math.max(0, ModConfig.getPrintBaseMaterialCost());

        int normalCount = Math.max(0, counts.totalNonEmpty() - counts.transparentCount() - counts.emissiveCount());
        double weighted = normalCount
                + counts.transparentCount() * transMult
                + counts.emissiveCount() * emissiveMult;
        // 避免透明兼发光体素扣减过头导致 weighted 小于 nonEmpty（当系数>=1时保持至少按普通体素计）
        if (transMult >= 1.0 && emissiveMult >= 1.0) {
            weighted = Math.max(weighted, counts.totalNonEmpty());
        }

        int cost;
        if (counts.totalNonEmpty() <= 0) {
            cost = 0;
        } else {
            int rawPerUnit = (int) Math.ceil(weighted / voxelsPerMat);
            cost = Math.max(1, (baseCost + rawPerUnit) * scale);
        }

        ModLog.debug("[CanvasPrintCalculator] 计算打印消耗: type=" + data.getType()
                + ", size=" + data.getSizeX() + "x" + data.getSizeY() + "x" + data.getSizeZ()
                + ", nonEmpty=" + counts.totalNonEmpty()
                + ", transparent=" + counts.transparentCount()
                + ", emissive=" + counts.emissiveCount()
                + ", limit=" + limit
                + ", exceeds=" + exceeds
                + ", scale=" + scale
                + ", weighted=" + String.format(java.util.Locale.ROOT, "%.2f", weighted)
                + ", cost=" + cost);

        return new PrintCostReport(
                counts.totalNonEmpty(),
                counts.transparentCount(),
                counts.emissiveCount(),
                limit,
                exceeds,
                scale,
                weighted,
                cost
        );
    }

    public static int calculateMetadataFallbackCost(CanvasMetadata meta) {
        if (meta == null || meta.nonEmptyVoxels() <= 0) {
            return 0;
        }
        int voxelsPerMat = Math.max(1, ModConfig.getVoxelsPerPrintMaterial());
        int baseCost = Math.max(0, ModConfig.getPrintBaseMaterialCost());
        int scale = Math.max(1, meta.printScale());
        int raw = (int) Math.ceil((double) meta.nonEmptyVoxels() / voxelsPerMat);
        return Math.max(1, (baseCost + raw) * scale);
    }
}
