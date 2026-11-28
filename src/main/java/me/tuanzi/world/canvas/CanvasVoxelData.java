package me.tuanzi.world.canvas;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 稀疏画布体素数据容器：
 * 仅存储非空体素坐标与调色板索引（采用位打包坐标 y<<16 | z<<8 | x，支持最大 256×256×256）。
 */
public final class CanvasVoxelData {
    private final int sizeX;
    private final int sizeY;
    private final int sizeZ;
    private final CanvasType type;
    private final List<CanvasPaletteEntry> palette;
    private final Map<CanvasPaletteEntry, Integer> paletteLookup;
    private final Int2IntOpenHashMap voxels;

    public CanvasVoxelData(int sizeX, int sizeY, int sizeZ, CanvasType type) {
        this.sizeX = Math.max(1, Math.min(256, sizeX));
        this.sizeY = Math.max(1, Math.min(256, sizeY));
        this.sizeZ = Math.max(1, Math.min(256, sizeZ));
        this.type = type != null ? type : CanvasType.UNSET;
        this.palette = new ArrayList<>();
        this.paletteLookup = new HashMap<>();
        this.voxels = new Int2IntOpenHashMap();
        this.voxels.defaultReturnValue(-1);
    }

    public static CanvasVoxelData createEmpty(int size, CanvasType type) {
        int h = (type == CanvasType.VOXEL_3D) ? Math.min(size, CanvasType.MAX_SIZE_3D) : 1;
        return new CanvasVoxelData(size, h, size, type);
    }

    public static int packCoord(int x, int y, int z) {
        return ((y & 0xFF) << 16) | ((z & 0xFF) << 8) | (x & 0xFF);
    }

    public static int unpackX(int packed) {
        return packed & 0xFF;
    }

    public static int unpackY(int packed) {
        return (packed >>> 16) & 0xFF;
    }

    public static int unpackZ(int packed) {
        return (packed >>> 8) & 0xFF;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    public CanvasType getType() {
        return type;
    }

    public List<CanvasPaletteEntry> getPalette() {
        return palette;
    }

    public Int2IntOpenHashMap getVoxelMap() {
        return voxels;
    }

    public int getNonEmptyCount() {
        return voxels.size();
    }

    public boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < sizeX && y >= 0 && y < sizeY && z >= 0 && z < sizeZ;
    }

    public void setVoxel(int x, int y, int z, CanvasPaletteEntry entry) {
        if (!isInBounds(x, y, z) || entry == null || "minecraft:air".equals(entry.blockStateStr())) {
            if (isInBounds(x, y, z)) {
                voxels.remove(packCoord(x, y, z));
            }
            return;
        }
        int idx = getOrAddPaletteIndex(entry);
        voxels.put(packCoord(x, y, z), idx);
    }

    public void setVoxelRaw(int x, int y, int z, int paletteIdx) {
        if (!isInBounds(x, y, z) || paletteIdx < 0 || paletteIdx >= palette.size()) {
            return;
        }
        voxels.put(packCoord(x, y, z), paletteIdx);
    }

    public int getOrAddPaletteIndex(CanvasPaletteEntry entry) {
        Integer existing = paletteLookup.get(entry);
        if (existing != null) {
            return existing;
        }
        int idx = palette.size();
        palette.add(entry);
        paletteLookup.put(entry, idx);
        return idx;
    }

    public CanvasPaletteEntry getEntry(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return null;
        }
        int idx = voxels.get(packCoord(x, y, z));
        if (idx < 0 || idx >= palette.size()) {
            return null;
        }
        return palette.get(idx);
    }

    public int getPaletteIndex(int x, int y, int z) {
        if (!isInBounds(x, y, z)) {
            return -1;
        }
        return voxels.get(packCoord(x, y, z));
    }

    /**
     * 升级画布尺寸时将旧体素数据居中搬移至更大的尺寸空间内。
     */
    public CanvasVoxelData centerExpand(int newSize) {
        int newY = (this.type == CanvasType.VOXEL_3D) ? Math.min(newSize, CanvasType.MAX_SIZE_3D) : 1;
        CanvasVoxelData expanded = new CanvasVoxelData(newSize, newY, newSize, this.type);
        int offsetX = Math.max(0, (newSize - this.sizeX) / 2);
        int offsetY = (this.type == CanvasType.VOXEL_3D) ? Math.max(0, (newY - this.sizeY) / 2) : 0;
        int offsetZ = Math.max(0, (newSize - this.sizeZ) / 2);

        for (Int2IntMap.Entry entry : this.voxels.int2IntEntrySet()) {
            int packed = entry.getIntKey();
            int pIdx = entry.getIntValue();
            if (pIdx < 0 || pIdx >= this.palette.size()) {
                continue;
            }
            int x = unpackX(packed) + offsetX;
            int y = unpackY(packed) + offsetY;
            int z = unpackZ(packed) + offsetZ;
            expanded.setVoxel(x, y, z, this.palette.get(pIdx));
        }
        return expanded;
    }

    public record VoxelClassificationCounts(int totalNonEmpty, int transparentCount, int emissiveCount) {
    }

    public VoxelClassificationCounts classifyVoxels() {
        boolean[] transparentCache = new boolean[palette.size()];
        boolean[] emissiveCache = new boolean[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            CanvasPaletteEntry pe = palette.get(i);
            BlockState state = pe.resolveBlockState();
            transparentCache[i] = !state.canOcclude() || !state.getFluidState().isEmpty();
            emissiveCache[i] = state.getLightEmission() > 0;
        }

        int total = 0;
        int transparent = 0;
        int emissive = 0;
        for (Int2IntMap.Entry e : voxels.int2IntEntrySet()) {
            int idx = e.getIntValue();
            if (idx < 0 || idx >= palette.size()) {
                continue;
            }
            total++;
            if (transparentCache[idx]) {
                transparent++;
            }
            if (emissiveCache[idx]) {
                emissive++;
            }
        }
        return new VoxelClassificationCounts(total, transparent, emissive);
    }

    public String computeSha256Hex() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteBuffer header = ByteBuffer.allocate(16);
            header.putInt(sizeX).putInt(sizeY).putInt(sizeZ).putInt(type.ordinal());
            md.update(header.array());
            for (CanvasPaletteEntry pe : palette) {
                md.update(pe.blockStateStr().getBytes(StandardCharsets.UTF_8));
                md.update((byte) 0);
                ByteBuffer col = ByteBuffer.allocate(8);
                col.putInt(pe.colorRgb()).putInt(pe.altColorRgb());
                md.update(col.array());
            }
            int[] sortedCoords = voxels.keySet().toIntArray();
            java.util.Arrays.sort(sortedCoords);
            ByteBuffer kv = ByteBuffer.allocate(8);
            for (int coord : sortedCoords) {
                kv.clear();
                kv.putInt(coord).putInt(voxels.get(coord));
                md.update(kv.array());
            }
            byte[] digest = md.digest();
            StringBuilder hex = new StringBuilder(16);
            for (int i = 0; i < 8 && i < digest.length; i++) {
                hex.append(String.format("%02x", digest[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(voxels.size() * 31 + sizeX);
        }
    }
}
