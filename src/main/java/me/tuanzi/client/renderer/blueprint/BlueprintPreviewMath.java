package me.tuanzi.client.renderer.blueprint;

import com.mojang.blaze3d.platform.InputConstants;

/**
 * 蓝图 3D 检视器纯数学与交互辅助工具：
 * 统一 3D 视口投影、缩放深度压缩、左下角罗盘命中检测与鼠标按键映射。
 */
public final class BlueprintPreviewMath {

    public static final int TOP_BAR_HEIGHT = 26;
    public static final int BOTTOM_BAR_HEIGHT = 24;
    public static final int COMPASS_HALF_SIZE = 24;
    public static final int COMPASS_CENTER_X = 34;

    private BlueprintPreviewMath() {
    }

    public static int getCompassCenterY(int viewportHeight) {
        return viewportHeight - BOTTOM_BAR_HEIGHT - 32;
    }

    public static boolean isPointInCompass(double mouseX, double mouseY, int viewportHeight) {
        int compX = COMPASS_CENTER_X;
        int compY = getCompassCenterY(viewportHeight);
        return mouseX >= compX - COMPASS_HALF_SIZE
                && mouseX <= compX + COMPASS_HALF_SIZE
                && mouseY >= compY - COMPASS_HALF_SIZE
                && mouseY <= compY + COMPASS_HALF_SIZE;
    }

    /**
     * 左键拖拽旋转视角（所有视角模式含等轴均支持）。
     * Minecraft 26.3 使用 SDL3 按键编码：左键=1，中键=2，右键=3。
     */
    public static boolean isOrbitDragButton(int button) {
        return button == InputConstants.MOUSE_BUTTON_LEFT;
    }

    /**
     * 右键或中键拖拽平移视口。
     * Minecraft 26.3 使用 SDL3 按键编码：中键=2，右键=3。
     */
    public static boolean isPanDragButton(int button) {
        return button == InputConstants.MOUSE_BUTTON_RIGHT || button == InputConstants.MOUSE_BUTTON_MIDDLE;
    }

    /**
     * 限制视口平移偏移量，确保结构包围区域至少保留部分在视口之内，防止意外拖出裁剪边界导致渲染消失。
     */
    public static float clampPan(float pan, int viewportSpan, float modelScreenRadius) {
        float maxOffset = Math.max(40.0F, viewportSpan * 0.45F + modelScreenRadius * 0.85F);
        return Math.clamp(pan, -maxOffset, maxOffset);
    }

    public static float computeBaseScale(int vpWidth, int vpHeight, int structWidth, int structHeight, int structLength, float zoom) {
        int usableH = Math.max(40, vpHeight - TOP_BAR_HEIGHT - BOTTOM_BAR_HEIGHT);
        float maxDim = Math.max(structWidth, Math.max(structHeight, structLength));
        return (Math.min(vpWidth, usableH) * 0.62F / Math.max(1.0F, maxDim)) * zoom;
    }

    /**
     * 当结构较大或缩放倍率较高时，压缩 Z 轴深度以确保全部几何体落在 PictureInPictureRenderer 的 [-1000, 1000] 正交深度范围内，避免被近/远平面裁剪。
     */
    public static float computeZCompress(int structWidth, int structHeight, int structLength, float baseScale, int guiScale) {
        double halfRadius = 0.5 * Math.sqrt(
                (double) structWidth * structWidth
                        + (double) structHeight * structHeight
                        + (double) structLength * structLength
        );
        // 额外预留 4 个方块余量覆盖 X/Y/Z 标尺线与多方块外伸部分，取消 0.02 下限避免高缩放或大结构触发深度裁切
        double maxDepth = (halfRadius + 4.0) * Math.max(0.01F, baseScale) * Math.max(1, guiScale);
        if (maxDepth <= 800.0) {
            return 1.0F;
        }
        return (float) Math.max(1.0E-4, 800.0 / maxDepth);
    }

    /**
     * 将以结构中心为原点的局部 3D 坐标投影到 GUI 屏幕像素坐标。
     * 与 BlueprintPreviewRenderer 的 PoseStack 变换完全对齐。
     */
    public static void projectToScreen(
            float wx, float wy, float wz,
            float cosY, float sinY, float cosP, float sinP,
            float scale, float cx, float cy,
            float[] outXY
    ) {
        float rx = wx * cosY - wz * sinY;
        float rz = wx * sinY + wz * cosY;
        float ry = wy * cosP - rz * sinP;
        outXY[0] = cx + rx * scale;
        outXY[1] = cy - ry * scale;
    }
}
