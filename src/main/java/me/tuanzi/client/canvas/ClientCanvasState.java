package me.tuanzi.client.canvas;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;

/**
 * 客户端当前画布编辑空间状态快照。
 */
@Environment(EnvType.CLIENT)
public final class ClientCanvasState {
    private static volatile boolean inCanvasSpace = false;
    private static volatile BlockPos origin = BlockPos.ZERO;
    private static volatile int sizeX = 0;
    private static volatile int sizeY = 0;
    private static volatile int sizeZ = 0;
    private static volatile boolean showBorder = false;
    private static volatile boolean readOnly = false;
    private static volatile boolean creativeMode = false;

    private ClientCanvasState() {
    }

    public static void update(
            boolean inSpace,
            BlockPos newOrigin,
            int sx,
            int sy,
            int sz,
            boolean border,
            boolean ro,
            boolean creative
    ) {
        inCanvasSpace = inSpace;
        origin = newOrigin != null ? newOrigin : BlockPos.ZERO;
        sizeX = sx;
        sizeY = sy;
        sizeZ = sz;
        showBorder = border;
        readOnly = ro;
        creativeMode = creative;
    }

    public static void clear() {
        update(false, BlockPos.ZERO, 0, 0, 0, false, false, false);
    }

    public static boolean isInCanvasSpace() {
        return inCanvasSpace;
    }

    public static BlockPos getOrigin() {
        return origin;
    }

    public static int getSizeX() {
        return sizeX;
    }

    public static int getSizeY() {
        return sizeY;
    }

    public static int getSizeZ() {
        return sizeZ;
    }

    public static boolean isShowBorder() {
        return showBorder;
    }

    public static boolean isReadOnly() {
        return readOnly;
    }

    public static boolean isCreativeMode() {
        return creativeMode;
    }
}
