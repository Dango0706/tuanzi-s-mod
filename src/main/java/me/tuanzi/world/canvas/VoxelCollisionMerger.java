package me.tuanzi.world.canvas;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;

/**
 * 体素碰撞箱 3D 贪婪合并引擎：
 * 将任意尺寸画布映射为每个占用世界方块内的 [0..16]³ 合并 AABB 列表，并设硬上限自动降采样以防性能退化。
 */
public final class VoxelCollisionMerger {
    public static final int SUB_RES = 16;
    public static final int MAX_BOXES_PER_CELL = 32;

    private VoxelCollisionMerger() {
    }

    /**
     * 以 1/16 方块为最小单位的整型盒 [minX, minY, minZ] 到 [maxX, maxY, maxZ)（范围 0..16）。
     */
    public record Box16(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public VoxelShape toVoxelShape() {
            return Shapes.box(
                    minX / 16.0, minY / 16.0, minZ / 16.0,
                    maxX / 16.0, maxY / 16.0, maxZ / 16.0
            );
        }
    }

    /**
     * 计算指定世界占用单元 (cellX, cellY, cellZ) 内的合并碰撞盒列表。
     *
     * @param data       画布体素数据
     * @param worldCellsX 世界占用 X 格数（printScale）
     * @param worldCellsY 世界占用 Y 格数（2D 为 1，3D 为 printScale）
     * @param worldCellsZ 世界占用 Z 格数（printScale）
     * @param cellX      当前单元 X (0..worldCellsX-1)
     * @param cellY      当前单元 Y (0..worldCellsY-1)
     * @param cellZ      当前单元 Z (0..worldCellsZ-1)
     */
    public static List<Box16> computeCellBoxes(
            CanvasVoxelData data,
            int worldCellsX,
            int worldCellsY,
            int worldCellsZ,
            int cellX,
            int cellY,
            int cellZ
    ) {
        if (data == null || data.getNonEmptyCount() == 0) {
            return List.of();
        }
        boolean[] occ = new boolean[SUB_RES * SUB_RES * SUB_RES];
        boolean any = false;

        int sx = data.getSizeX();
        int sy = data.getSizeY();
        int sz = data.getSizeZ();

        double totalSubX = (double) worldCellsX * SUB_RES;
        double totalSubY = (double) worldCellsY * SUB_RES;
        double totalSubZ = (double) worldCellsZ * SUB_RES;

        int cellSubStartX = cellX * SUB_RES;
        int cellSubStartY = cellY * SUB_RES;
        int cellSubStartZ = cellZ * SUB_RES;

        for (Int2IntMap.Entry e : data.getVoxelMap().int2IntEntrySet()) {
            int packed = e.getIntKey();
            int vx = CanvasVoxelData.unpackX(packed);
            int vy = CanvasVoxelData.unpackY(packed);
            int vz = CanvasVoxelData.unpackZ(packed);

            int subX0 = (int) Math.floor(vx * totalSubX / sx);
            int subX1 = Math.max(subX0 + 1, (int) Math.ceil((vx + 1) * totalSubX / sx));
            int subY0 = (data.getType() == CanvasType.FLAT_2D)
                    ? 0
                    : (int) Math.floor(vy * totalSubY / sy);
            int subY1 = (data.getType() == CanvasType.FLAT_2D)
                    ? 1
                    : Math.max(subY0 + 1, (int) Math.ceil((vy + 1) * totalSubY / sy));
            int subZ0 = (int) Math.floor(vz * totalSubZ / sz);
            int subZ1 = Math.max(subZ0 + 1, (int) Math.ceil((vz + 1) * totalSubZ / sz));

            int lx0 = Math.max(0, subX0 - cellSubStartX);
            int lx1 = Math.min(SUB_RES, subX1 - cellSubStartX);
            int ly0 = Math.max(0, subY0 - cellSubStartY);
            int ly1 = Math.min(SUB_RES, subY1 - cellSubStartY);
            int lz0 = Math.max(0, subZ0 - cellSubStartZ);
            int lz1 = Math.min(SUB_RES, subZ1 - cellSubStartZ);

            if (lx0 >= lx1 || ly0 >= ly1 || lz0 >= lz1) {
                continue;
            }
            for (int y = ly0; y < ly1; y++) {
                for (int z = lz0; z < lz1; z++) {
                    int base = (y * SUB_RES + z) * SUB_RES;
                    for (int x = lx0; x < lx1; x++) {
                        occ[base + x] = true;
                        any = true;
                    }
                }
            }
        }

        if (!any) {
            return List.of();
        }

        List<Box16> merged = greedyMerge16(occ, 1);
        if (merged.size() <= MAX_BOXES_PER_CELL) {
            return merged;
        }
        // 超出上限时逐级降采样 (step=2 -> 8³, step=4 -> 4³)，最后退化为单外接盒
        List<Box16> down2 = greedyMerge16(downsample(occ, 2), 2);
        if (down2.size() <= MAX_BOXES_PER_CELL) {
            return down2;
        }
        List<Box16> down4 = greedyMerge16(downsample(occ, 4), 4);
        if (down4.size() <= MAX_BOXES_PER_CELL) {
            return down4;
        }
        return List.of(boundingBox(occ));
    }

    public static VoxelShape toVoxelShape(List<Box16> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return Shapes.empty();
        }
        VoxelShape shape = boxes.getFirst().toVoxelShape();
        for (int i = 1; i < boxes.size(); i++) {
            shape = Shapes.or(shape, boxes.get(i).toVoxelShape());
        }
        return shape;
    }

    private static List<Box16> greedyMerge16(boolean[] grid, int step) {
        int dim = SUB_RES / step;
        boolean[] visited = new boolean[dim * dim * dim];
        List<Box16> boxes = new ArrayList<>();

        for (int y = 0; y < dim; y++) {
            for (int z = 0; z < dim; z++) {
                for (int x = 0; x < dim; x++) {
                    int idx = (y * dim + z) * dim + x;
                    if (!grid[idx] || visited[idx]) {
                        continue;
                    }
                    int maxX = x + 1;
                    while (maxX < dim) {
                        int nIdx = (y * dim + z) * dim + maxX;
                        if (!grid[nIdx] || visited[nIdx]) {
                            break;
                        }
                        maxX++;
                    }
                    int maxZ = z + 1;
                    outerZ:
                    while (maxZ < dim) {
                        for (int ix = x; ix < maxX; ix++) {
                            int nIdx = (y * dim + maxZ) * dim + ix;
                            if (!grid[nIdx] || visited[nIdx]) {
                                break outerZ;
                            }
                        }
                        maxZ++;
                    }
                    int maxY = y + 1;
                    outerY:
                    while (maxY < dim) {
                        for (int iz = z; iz < maxZ; iz++) {
                            for (int ix = x; ix < maxX; ix++) {
                                int nIdx = (maxY * dim + iz) * dim + ix;
                                if (!grid[nIdx] || visited[nIdx]) {
                                    break outerY;
                                }
                            }
                        }
                        maxY++;
                    }
                    for (int iy = y; iy < maxY; iy++) {
                        for (int iz = z; iz < maxZ; iz++) {
                            for (int ix = x; ix < maxX; ix++) {
                                visited[(iy * dim + iz) * dim + ix] = true;
                            }
                        }
                    }
                    boxes.add(new Box16(
                            x * step, y * step, z * step,
                            maxX * step, maxY * step, maxZ * step
                    ));
                }
            }
        }
        return boxes;
    }

    private static boolean[] downsample(boolean[] src, int step) {
        int dim = SUB_RES / step;
        boolean[] dst = new boolean[dim * dim * dim];
        for (int y = 0; y < SUB_RES; y++) {
            for (int z = 0; z < SUB_RES; z++) {
                for (int x = 0; x < SUB_RES; x++) {
                    if (src[(y * SUB_RES + z) * SUB_RES + x]) {
                        int dx = x / step;
                        int dy = y / step;
                        int dz = z / step;
                        dst[(dy * dim + dz) * dim + dx] = true;
                    }
                }
            }
        }
        return dst;
    }

    private static Box16 boundingBox(boolean[] occ) {
        int minX = SUB_RES, minY = SUB_RES, minZ = SUB_RES;
        int maxX = 0, maxY = 0, maxZ = 0;
        for (int y = 0; y < SUB_RES; y++) {
            for (int z = 0; z < SUB_RES; z++) {
                for (int x = 0; x < SUB_RES; x++) {
                    if (occ[(y * SUB_RES + z) * SUB_RES + x]) {
                        minX = Math.min(minX, x);
                        minY = Math.min(minY, y);
                        minZ = Math.min(minZ, z);
                        maxX = Math.max(maxX, x + 1);
                        maxY = Math.max(maxY, y + 1);
                        maxZ = Math.max(maxZ, z + 1);
                    }
                }
            }
        }
        if (minX >= maxX) {
            return new Box16(0, 0, 0, 16, 16, 16);
        }
        return new Box16(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
