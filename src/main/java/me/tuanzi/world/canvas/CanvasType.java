package me.tuanzi.world.canvas;

/**
 * 画布维度类型：未定型、2D 平面（厚度 1）、3D 立体（最大 128³）。
 */
public enum CanvasType {
    UNSET("unset"),
    FLAT_2D("2d"),
    VOXEL_3D("3d");

    public static final int[] VALID_SIZES = {16, 32, 64, 128, 256};
    public static final int MAX_SIZE_2D = 256;
    public static final int MAX_SIZE_3D = 128;

    private final String id;

    CanvasType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static CanvasType fromId(String id) {
        if (id == null) {
            return UNSET;
        }
        for (CanvasType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return UNSET;
    }

    public static boolean isValidSize(int size, CanvasType type) {
        for (int valid : VALID_SIZES) {
            if (valid == size) {
                return type != VOXEL_3D || size <= MAX_SIZE_3D;
            }
        }
        return false;
    }

    public static int normalizeSize(int size, CanvasType type) {
        if (isValidSize(size, type)) {
            return size;
        }
        if (type == VOXEL_3D && size > MAX_SIZE_3D) {
            return MAX_SIZE_3D;
        }
        return 16;
    }

    public static boolean canUpgrade(int currentSize, CanvasType type) {
        return nextSize(currentSize, type) > currentSize;
    }

    public static int nextSize(int currentSize, CanvasType type) {
        int maxAllowed = (type == VOXEL_3D) ? MAX_SIZE_3D : MAX_SIZE_2D;
        for (int valid : VALID_SIZES) {
            if (valid > currentSize && valid <= maxAllowed) {
                return valid;
            }
        }
        return currentSize;
    }

    public int getHeightForSize(int size) {
        return switch (this) {
            case FLAT_2D, UNSET -> 1;
            case VOXEL_3D -> Math.min(size, MAX_SIZE_3D);
        };
    }

    public String getTranslationKey() {
        return "canvas.tuanzis_mod.type." + id;
    }
}
