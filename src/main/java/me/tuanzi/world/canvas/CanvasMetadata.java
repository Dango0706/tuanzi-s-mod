package me.tuanzi.world.canvas;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 存储在画布与打印模型物品 CustomData 中的轻量元数据（不含体素正文，避免 NBT 膨胀）。
 */
public record CanvasMetadata(
        @Nullable UUID uuid,
        int formatVersion,
        int size,
        CanvasType type,
        @Nullable UUID ownerUuid,
        String ownerName,
        CanvasPermission permission,
        int nonEmptyVoxels,
        long lastEditTime,
        String dataHash,
        boolean corrupted,
        int printScale,
        @Nullable UUID upgradeSourceUuid,
        int upgradeSourceSize
) {
    public static final int CURRENT_FORMAT_VERSION = 1;
    public static final String KEY_UUID = "CanvasUuid";
    public static final String KEY_FORMAT_VERSION = "CanvasFormatVersion";
    public static final String KEY_SIZE = "CanvasSize";
    public static final String KEY_TYPE = "CanvasType";
    public static final String KEY_OWNER_UUID = "OwnerUuid";
    public static final String KEY_OWNER_NAME = "OwnerName";
    public static final String KEY_PERMISSION = "Permission";
    public static final String KEY_NON_EMPTY_VOXELS = "NonEmptyVoxels";
    public static final String KEY_LAST_EDIT_TIME = "LastEditTime";
    public static final String KEY_DATA_HASH = "DataHash";
    public static final String KEY_CORRUPTED = "Corrupted";
    public static final String KEY_PRINT_SCALE = "PrintScale";
    public static final String KEY_UPGRADE_SOURCE_UUID = "UpgradeSourceUuid";
    public static final String KEY_UPGRADE_SOURCE_SIZE = "UpgradeSourceSize";

    public static CanvasMetadata createDefaultBlank() {
        return new CanvasMetadata(
                null,
                CURRENT_FORMAT_VERSION,
                16,
                CanvasType.UNSET,
                null,
                "",
                CanvasPermission.PRIVATE,
                0,
                0L,
                "",
                false,
                1,
                null,
                0
        );
    }

    public static CanvasMetadata read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return createDefaultBlank();
        }
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return createDefaultBlank();
        }
        return readFromTag(customData.copyTag());
    }

    public static CanvasMetadata readFromTag(CompoundTag tag) {
        if (tag == null || tag.isEmpty()) {
            return createDefaultBlank();
        }
        UUID uuid = parseUuidOrNull(tag.getStringOr(KEY_UUID, ""));
        int formatVersion = tag.getIntOr(KEY_FORMAT_VERSION, CURRENT_FORMAT_VERSION);
        CanvasType type = CanvasType.fromId(tag.getStringOr(KEY_TYPE, CanvasType.UNSET.getId()));
        int size = CanvasType.normalizeSize(tag.getIntOr(KEY_SIZE, 16), type);
        UUID ownerUuid = parseUuidOrNull(tag.getStringOr(KEY_OWNER_UUID, ""));
        String ownerName = tag.getStringOr(KEY_OWNER_NAME, "");
        CanvasPermission permission = CanvasPermission.fromId(tag.getStringOr(KEY_PERMISSION, CanvasPermission.PRIVATE.getId()));
        int nonEmptyVoxels = Math.max(0, tag.getIntOr(KEY_NON_EMPTY_VOXELS, 0));
        long lastEditTime = tag.getLongOr(KEY_LAST_EDIT_TIME, 0L);
        String dataHash = tag.getStringOr(KEY_DATA_HASH, "");
        boolean corrupted = tag.getBooleanOr(KEY_CORRUPTED, false);
        int printScale = Math.max(1, tag.getIntOr(KEY_PRINT_SCALE, 1));
        UUID upgradeSourceUuid = parseUuidOrNull(tag.getStringOr(KEY_UPGRADE_SOURCE_UUID, ""));
        int upgradeSourceSize = Math.max(0, tag.getIntOr(KEY_UPGRADE_SOURCE_SIZE, 0));

        return new CanvasMetadata(
                uuid, formatVersion, size, type, ownerUuid, ownerName,
                permission, nonEmptyVoxels, lastEditTime, dataHash,
                corrupted, printScale, upgradeSourceUuid, upgradeSourceSize
        );
    }

    public static void write(ItemStack stack, CanvasMetadata meta) {
        if (stack == null || stack.isEmpty() || meta == null) {
            return;
        }
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        writeToTag(tag, meta);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public static void writeToTag(CompoundTag tag, CanvasMetadata meta) {
        if (meta.uuid != null) {
            tag.putString(KEY_UUID, meta.uuid.toString());
        } else {
            tag.remove(KEY_UUID);
        }
        tag.putInt(KEY_FORMAT_VERSION, meta.formatVersion);
        tag.putInt(KEY_SIZE, meta.size);
        tag.putString(KEY_TYPE, meta.type.getId());
        if (meta.ownerUuid != null) {
            tag.putString(KEY_OWNER_UUID, meta.ownerUuid.toString());
        } else {
            tag.remove(KEY_OWNER_UUID);
        }
        tag.putString(KEY_OWNER_NAME, meta.ownerName != null ? meta.ownerName : "");
        tag.putString(KEY_PERMISSION, meta.permission.getId());
        tag.putInt(KEY_NON_EMPTY_VOXELS, Math.max(0, meta.nonEmptyVoxels));
        tag.putLong(KEY_LAST_EDIT_TIME, meta.lastEditTime);
        tag.putString(KEY_DATA_HASH, meta.dataHash != null ? meta.dataHash : "");
        tag.putBoolean(KEY_CORRUPTED, meta.corrupted);
        tag.putInt(KEY_PRINT_SCALE, Math.max(1, meta.printScale));
        if (meta.upgradeSourceUuid != null) {
            tag.putString(KEY_UPGRADE_SOURCE_UUID, meta.upgradeSourceUuid.toString());
            tag.putInt(KEY_UPGRADE_SOURCE_SIZE, meta.upgradeSourceSize);
        } else {
            tag.remove(KEY_UPGRADE_SOURCE_UUID);
            tag.remove(KEY_UPGRADE_SOURCE_SIZE);
        }
    }

    public boolean hasAssignedUuid() {
        return uuid != null;
    }

    public boolean hasPendingUpgrade() {
        return upgradeSourceUuid != null && upgradeSourceSize > 0;
    }

    public CanvasMetadata withUuid(UUID newUuid) {
        return new CanvasMetadata(newUuid, formatVersion, size, type, ownerUuid, ownerName, permission,
                nonEmptyVoxels, lastEditTime, dataHash, corrupted, printScale, upgradeSourceUuid, upgradeSourceSize);
    }

    public CanvasMetadata withTypeAndOwner(CanvasType newType, UUID newOwnerUuid, String newOwnerName) {
        return new CanvasMetadata(uuid, formatVersion, size, newType, newOwnerUuid, newOwnerName, permission,
                nonEmptyVoxels, lastEditTime, dataHash, corrupted, printScale, upgradeSourceUuid, upgradeSourceSize);
    }

    public CanvasMetadata withPermission(CanvasPermission newPermission) {
        return new CanvasMetadata(uuid, formatVersion, size, type, ownerUuid, ownerName, newPermission,
                nonEmptyVoxels, lastEditTime, dataHash, corrupted, printScale, upgradeSourceUuid, upgradeSourceSize);
    }

    public CanvasMetadata withStats(int newNonEmptyVoxels, long newLastEditTime, String newHash, boolean newCorrupted) {
        return new CanvasMetadata(uuid, formatVersion, size, type, ownerUuid, ownerName, permission,
                newNonEmptyVoxels, newLastEditTime, newHash, newCorrupted, printScale, null, 0);
    }

    public CanvasMetadata withPrintSnapshot(UUID modelUuid, int scale) {
        return new CanvasMetadata(modelUuid, CURRENT_FORMAT_VERSION, size, type, ownerUuid, ownerName, permission,
                nonEmptyVoxels, lastEditTime, dataHash, false, Math.max(1, scale), null, 0);
    }

    public CanvasMetadata withUpgradePending(UUID newUuid, int newSize, UUID srcUuid, int srcSize) {
        return new CanvasMetadata(newUuid, CURRENT_FORMAT_VERSION, newSize, type, ownerUuid, ownerName, permission,
                nonEmptyVoxels, lastEditTime, dataHash, corrupted, printScale, srcUuid, srcSize);
    }

    private static @Nullable UUID parseUuidOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
