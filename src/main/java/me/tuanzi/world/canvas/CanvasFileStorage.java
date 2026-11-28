package me.tuanzi.world.canvas;

import me.tuanzi.util.ModLog;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 画布持久化文件存储管理器：
 * 所有体素压缩数据统一存放在游戏根目录 painted/ 下（<uuid>.painted / <uuid>.tmp.painted）。
 * 严禁调用任何删除文件 API，丢弃草稿采用覆写清除标记文件实现。
 */
public final class CanvasFileStorage {
    private static volatile @Nullable Path overrideBaseDir = null;

    private CanvasFileStorage() {
    }

    public static void setOverrideBaseDirForTest(@Nullable Path dir) {
        overrideBaseDir = dir;
    }

    public static Path getPaintedDir() {
        if (overrideBaseDir != null) {
            return overrideBaseDir;
        }
        return FabricLoader.getInstance().getGameDir().resolve("painted");
    }

    public static Path getSavedFile(UUID uuid) {
        return getPaintedDir().resolve(uuid.toString() + ".painted");
    }

    public static Path getDraftFile(UUID uuid) {
        return getPaintedDir().resolve(uuid.toString() + ".tmp.painted");
    }

    public static Path getExportsDir() {
        return getPaintedDir().resolve("exports");
    }

    public static void ensureDirectories() throws IOException {
        Files.createDirectories(getPaintedDir());
    }

    public static boolean hasSavedFile(UUID uuid) {
        return uuid != null && Files.isRegularFile(getSavedFile(uuid));
    }

    public static boolean hasActiveDraft(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        Path draft = getDraftFile(uuid);
        if (!Files.isRegularFile(draft)) {
            return false;
        }
        try {
            byte[] bytes = Files.readAllBytes(draft);
            return bytes.length > 0 && !CanvasDataCodec.isClearedDraftMarker(bytes);
        } catch (IOException e) {
            return false;
        }
    }

    public static CanvasDataCodec.DecodeResult loadSaved(UUID uuid) {
        return loadFromPath(getSavedFile(uuid));
    }

    public static CanvasDataCodec.DecodeResult loadDraftOrSaved(UUID uuid, int fallbackSize, CanvasType fallbackType) {
        if (uuid != null && hasActiveDraft(uuid)) {
            CanvasDataCodec.DecodeResult draftRes = loadFromPath(getDraftFile(uuid));
            if (draftRes.isOk()) {
                return draftRes;
            }
            if (draftRes.corrupted()) {
                return draftRes;
            }
        }
        if (uuid != null && hasSavedFile(uuid)) {
            return loadFromPath(getSavedFile(uuid));
        }
        return new CanvasDataCodec.DecodeResult(
                CanvasVoxelData.createEmpty(fallbackSize, fallbackType),
                CanvasDataCodec.CURRENT_VERSION,
                false,
                ""
        );
    }

    public static CanvasDataCodec.DecodeResult loadFromPath(Path path) {
        if (!Files.isRegularFile(path)) {
            return new CanvasDataCodec.DecodeResult(null, 0, true, "画布数据文件不存在: " + path.getFileName());
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return CanvasDataCodec.decode(bytes);
        } catch (IOException e) {
            return new CanvasDataCodec.DecodeResult(null, 0, true, "读取画布文件失败: " + e.getMessage());
        }
    }

    public static byte[] readRawSavedBytes(UUID uuid) throws IOException {
        Path path = getSavedFile(uuid);
        if (!Files.isRegularFile(path)) {
            return new byte[0];
        }
        return Files.readAllBytes(path);
    }

    public static void writeSaved(UUID uuid, CanvasVoxelData data) throws IOException {
        writeEncoded(getSavedFile(uuid), CanvasDataCodec.encode(data));
    }

    public static void writeRawSavedBytes(UUID uuid, byte[] encoded) throws IOException {
        writeEncoded(getSavedFile(uuid), encoded);
    }

    public static void writeDraft(UUID uuid, CanvasVoxelData data) throws IOException {
        writeEncoded(getDraftFile(uuid), CanvasDataCodec.encode(data));
    }

    public static void clearDraftMarker(UUID uuid) {
        if (uuid == null) {
            return;
        }
        Path draft = getDraftFile(uuid);
        if (!Files.exists(draft)) {
            return;
        }
        try {
            writeEncoded(draft, CanvasDataCodec.encodeClearedDraftMarker());
        } catch (IOException e) {
            ModLog.error("[CanvasFileStorage] 写入草稿清除标记失败: " + draft, e);
        }
    }

    private static void writeEncoded(Path target, byte[] bytes) throws IOException {
        ensureDirectories();
        Path part = target.resolveSibling(target.getFileName().toString() + ".part");
        Files.write(part, bytes);
        Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 物化延迟升级的画布：将旧 UUID 数据居中扩展并写入新 UUID 文件，彻底保留原有作品。
     */
    public static CanvasMetadata ensureMaterialized(ItemStack stack) {
        CanvasMetadata meta = CanvasMetadata.read(stack);
        if (!meta.hasPendingUpgrade()) {
            return meta;
        }
        UUID newUuid = meta.uuid() != null ? meta.uuid() : UUID.randomUUID();
        UUID srcUuid = meta.upgradeSourceUuid();
        int newSize = meta.size();
        CanvasType type = meta.type();

        CanvasVoxelData expanded;
        boolean corrupted = meta.corrupted();
        if (srcUuid != null && (hasActiveDraft(srcUuid) || hasSavedFile(srcUuid))) {
            CanvasDataCodec.DecodeResult srcRes = loadDraftOrSaved(srcUuid, meta.upgradeSourceSize(), type);
            if (srcRes.isOk() && srcRes.data() != null) {
                expanded = srcRes.data().centerExpand(newSize);
            } else {
                expanded = CanvasVoxelData.createEmpty(newSize, type);
                corrupted = srcRes.corrupted();
            }
        } else {
            expanded = CanvasVoxelData.createEmpty(newSize, type);
        }

        String hash = "";
        int nonEmpty = expanded.getNonEmptyCount();
        long now = System.currentTimeMillis();
        try {
            writeSaved(newUuid, expanded);
            hash = expanded.computeSha256Hex();
            ModLog.debug("[CanvasFileStorage] 画布升级物化完成: src=" + srcUuid + " (" + meta.upgradeSourceSize()
                    + ") -> dst=" + newUuid + " (" + newSize + "), nonEmpty=" + nonEmpty);
        } catch (IOException e) {
            ModLog.error("[CanvasFileStorage] 画布升级物化写入失败: " + newUuid, e);
            corrupted = true;
        }

        CanvasMetadata updated = new CanvasMetadata(
                newUuid,
                CanvasMetadata.CURRENT_FORMAT_VERSION,
                newSize,
                type,
                meta.ownerUuid(),
                meta.ownerName(),
                meta.permission(),
                nonEmpty,
                now,
                hash,
                corrupted,
                meta.printScale(),
                null,
                0
        );
        CanvasMetadata.write(stack, updated);
        return updated;
    }

    /**
     * 为打印模型生成一份不可变的独立 .painted 快照，避免后续修改画布影响已打印模型。
     */
    public static boolean createImmutablePrintSnapshot(UUID sourceCanvasUuid, UUID newModelUuid) {
        if (sourceCanvasUuid == null || newModelUuid == null) {
            return false;
        }
        try {
            ensureDirectories();
            Path src = getSavedFile(sourceCanvasUuid);
            if (!Files.isRegularFile(src)) {
                return false;
            }
            Path dst = getSavedFile(newModelUuid);
            Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
            ModLog.debug("[CanvasFileStorage] 创建打印模型不可变快照: " + sourceCanvasUuid + " -> " + newModelUuid);
            return true;
        } catch (IOException e) {
            ModLog.error("[CanvasFileStorage] 创建打印快照失败: " + sourceCanvasUuid + " -> " + newModelUuid, e);
            return false;
        }
    }

    /**
     * 导出损坏画布数据副本至 painted/exports/ 目录以便玩家排查修复。
     */
    public static @Nullable Path exportCorruptedCopy(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        try {
            Files.createDirectories(getExportsDir());
            Path src = Files.isRegularFile(getDraftFile(uuid)) ? getDraftFile(uuid) : getSavedFile(uuid);
            if (!Files.isRegularFile(src)) {
                return null;
            }
            Path out = getExportsDir().resolve(uuid + "_" + System.currentTimeMillis() + ".painted.export");
            Files.copy(src, out, StandardCopyOption.REPLACE_EXISTING);
            ModLog.debug("[CanvasFileStorage] 已导出损坏画布副本: " + out);
            return out;
        } catch (IOException e) {
            ModLog.error("[CanvasFileStorage] 导出损坏画布失败: " + uuid, e);
            return null;
        }
    }

    /**
     * 彻底损毁画布时将磁盘数据文件（正式版与草稿）重命名为 .bak 归档，严禁删除任何文件。
     */
    public static void archiveToBak(@Nullable UUID uuid) {
        if (uuid == null) {
            return;
        }
        archiveSingleFile(getSavedFile(uuid));
        archiveSingleFile(getDraftFile(uuid));
    }

    private static void archiveSingleFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }
        String baseName = file.getFileName().toString();
        Path bakPath = file.resolveSibling(baseName + ".bak");
        if (Files.exists(bakPath)) {
            bakPath = file.resolveSibling(baseName + "." + System.currentTimeMillis() + ".bak");
        }
        try {
            Files.move(file, bakPath, StandardCopyOption.REPLACE_EXISTING);
            ModLog.debug("[CanvasFileStorage] 已归档画布文件为 .bak: " + bakPath.getFileName());
        } catch (IOException e) {
            ModLog.error("[CanvasFileStorage] 归档画布文件失败: " + file, e);
        }
    }
}
