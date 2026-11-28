package me.tuanzi.world.canvas;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 画布体素二进制压缩编解码器（调色板 + 稀疏体素数组 + GZIP）。
 * 格式版本校验失败或内容损坏时安全返回只读损坏标识，绝不抛出未捕获异常崩溃。
 */
public final class CanvasDataCodec {
    public static final int MAGIC = 0x54504E54; // 'TPNT'
    public static final int CLEARED_DRAFT_MAGIC = 0x54434C52; // 'TCLR'
    public static final int CURRENT_VERSION = 1;
    public static final int MAX_DECOMPRESSED_ENTRIES = 128 * 128 * 128 + 1024;

    private CanvasDataCodec() {
    }

    public record DecodeResult(
            @Nullable CanvasVoxelData data,
            int fileVersion,
            boolean corrupted,
            String errorReason
    ) {
        public boolean isOk() {
            return data != null && !corrupted;
        }
    }

    public static byte[] encode(CanvasVoxelData data) throws IOException {
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(rawOut);
             DataOutputStream out = new DataOutputStream(gzip)) {
            out.writeInt(MAGIC);
            out.writeInt(CURRENT_VERSION);
            out.writeInt(data.getSizeX());
            out.writeInt(data.getSizeY());
            out.writeInt(data.getSizeZ());
            out.writeUTF(data.getType().getId());

            var palette = data.getPalette();
            out.writeInt(palette.size());
            for (CanvasPaletteEntry entry : palette) {
                out.writeUTF(entry.blockStateStr());
                out.writeInt(entry.colorRgb());
                out.writeInt(entry.altColorRgb());
            }

            var voxels = data.getVoxelMap();
            out.writeInt(voxels.size());
            for (Int2IntMap.Entry e : voxels.int2IntEntrySet()) {
                out.writeInt(e.getIntKey());
                out.writeInt(e.getIntValue());
            }
            out.flush();
        }
        return rawOut.toByteArray();
    }

    public static byte[] encodeClearedDraftMarker() throws IOException {
        ByteArrayOutputStream rawOut = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(rawOut)) {
            out.writeInt(CLEARED_DRAFT_MAGIC);
            out.writeInt(CURRENT_VERSION);
        }
        return rawOut.toByteArray();
    }

    public static boolean isClearedDraftMarker(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        int b0 = (bytes[0] & 0xFF) << 24;
        int b1 = (bytes[1] & 0xFF) << 16;
        int b2 = (bytes[2] & 0xFF) << 8;
        int b3 = bytes[3] & 0xFF;
        return (b0 | b1 | b2 | b3) == CLEARED_DRAFT_MAGIC;
    }

    public static DecodeResult decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return new DecodeResult(null, 0, true, "空画布数据");
        }
        if (isClearedDraftMarker(bytes)) {
            return new DecodeResult(null, CURRENT_VERSION, false, "已提交的空白草稿标记");
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             GZIPInputStream gzip = new GZIPInputStream(bais);
             DataInputStream in = new DataInputStream(gzip)) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                return new DecodeResult(null, 0, true, "非法的画布文件魔数: 0x" + Integer.toHexString(magic));
            }
            int version = in.readInt();
            if (version <= 0 || version > CURRENT_VERSION) {
                return new DecodeResult(null, version, true, "不兼容的画布格式版本: v" + version);
            }
            int sizeX = in.readInt();
            int sizeY = in.readInt();
            int sizeZ = in.readInt();
            if (sizeX <= 0 || sizeX > 256 || sizeY <= 0 || sizeY > 256 || sizeZ <= 0 || sizeZ > 256) {
                return new DecodeResult(null, version, true, "非法的画布尺寸: " + sizeX + "x" + sizeY + "x" + sizeZ);
            }
            CanvasType type = CanvasType.fromId(in.readUTF());
            if (type == CanvasType.VOXEL_3D && (sizeX > 128 || sizeY > 128 || sizeZ > 128)) {
                return new DecodeResult(null, version, true, "3D 画布尺寸超出 128 上限");
            }

            CanvasVoxelData data = new CanvasVoxelData(sizeX, sizeY, sizeZ, type);
            int paletteSize = in.readInt();
            if (paletteSize < 0 || paletteSize > MAX_DECOMPRESSED_ENTRIES) {
                return new DecodeResult(null, version, true, "非法的调色板大小: " + paletteSize);
            }
            for (int i = 0; i < paletteSize; i++) {
                String stateStr = in.readUTF();
                int colorRgb = in.readInt();
                int altColorRgb = in.readInt();
                data.getOrAddPaletteIndex(new CanvasPaletteEntry(stateStr, colorRgb, altColorRgb));
            }

            int voxelCount = in.readInt();
            if (voxelCount < 0 || voxelCount > MAX_DECOMPRESSED_ENTRIES) {
                return new DecodeResult(null, version, true, "非法的体素数量: " + voxelCount);
            }
            for (int i = 0; i < voxelCount; i++) {
                int packed = in.readInt();
                int paletteIdx = in.readInt();
                if (paletteIdx < 0 || paletteIdx >= paletteSize) {
                    return new DecodeResult(null, version, true, "体素索引越界: " + paletteIdx);
                }
                int x = CanvasVoxelData.unpackX(packed);
                int y = CanvasVoxelData.unpackY(packed);
                int z = CanvasVoxelData.unpackZ(packed);
                if (!data.isInBounds(x, y, z)) {
                    return new DecodeResult(null, version, true, "体素坐标越界: " + x + "," + y + "," + z);
                }
                data.setVoxelRaw(x, y, z, paletteIdx);
            }

            return new DecodeResult(data, version, false, "");
        } catch (Exception e) {
            return new DecodeResult(null, 0, true, "解码画布数据失败: " + e.getMessage());
        }
    }
}
