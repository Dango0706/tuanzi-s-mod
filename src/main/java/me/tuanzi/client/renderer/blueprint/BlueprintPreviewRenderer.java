package me.tuanzi.client.renderer.blueprint;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * 蓝图 3D 预览原生离屏渲染器（PictureInPictureRenderer）：
 * 拥有独立 RGBA8 + D32_FLOAT 硬件深度缓冲，统一渲染：
 * 1) 完整与非完整方块、流体（含水色与含水方块）的预烘焙网格；
 * 2) SpecialModel 兜底方块与模组多方块/告示牌等 BlockEntityRenderer；
 * 3) 受左下角罗盘开关控制的 3D 外边框与 X/Y/Z 坐标标尺线，以及切层截面高亮黄框。
 */
@Environment(EnvType.CLIENT)
public class BlueprintPreviewRenderer extends PictureInPictureRenderer<BlueprintPreviewRenderer.State> {

    private static final Set<Class<?>> WARNED_BER_CLASSES = new HashSet<>();

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected String getTextureLabel() {
        return "tuanzis_mod blueprint preview";
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected void renderToTexture(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);

        BlueprintPreviewMesh mesh = state.mesh();
        if (mesh == null || state.structWidth() <= 0 || state.structHeight() <= 0 || state.structLength() <= 0
                || state.scale() <= 0.0F || state.x1() <= state.x0() || state.y1() <= state.y0()) {
            return;
        }

        int guiScale = Math.max(1, mc.getWindow().getGuiScale());
        float zCompress = BlueprintPreviewMath.computeZCompress(
                state.structWidth(), state.structHeight(), state.structLength(), state.scale(), guiScale
        );

        poseStack.pushPose();
        try {
            // 屏幕像素空间平移
            poseStack.translate(state.panX() / state.scale(), state.panY() / state.scale(), 0.0F);
            if (zCompress < 1.0F) {
                poseStack.scale(1.0F, 1.0F, zCompress);
            }
            // 进入标准右手 3D 世界坐标方向：+X(东)右，+Y(上)上，+Z(南)朝向观察者
            poseStack.scale(1.0F, -1.0F, -1.0F);
            poseStack.rotate(Axis.XP.rotationDegrees(state.pitch()));
            poseStack.rotate(Axis.YP.rotationDegrees(-state.yaw()));
            poseStack.translate(-state.structWidth() / 2.0F, -state.structHeight() / 2.0F, -state.structLength() / 2.0F);

            Block selected = state.selectedBlock();

            // 1. 提交预烘焙固体/镂空方块网格
            if (mesh.solidBatch.hasAnyMatching(selected, true)) {
                collector.submitCustomGeometry(poseStack, RenderTypes.solidMovingBlock(),
                        (pose, vc) -> mesh.solidBatch.writeTo(pose, vc, selected, true, false));
            }
            if (mesh.cutoutBatch.hasAnyMatching(selected, true)) {
                collector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(),
                        (pose, vc) -> mesh.cutoutBatch.writeTo(pose, vc, selected, true, false));
            }

            // 2. 提交 SpecialModel 兜底方块（无 BlockEntityRenderer 接管的内置模型）
            for (BlueprintPreviewMesh.SpecialModelEntry entry : mesh.specialModels) {
                if (selected != null && entry.block() != selected) continue;
                poseStack.pushPose();
                try {
                    poseStack.translate((float) entry.x(), (float) entry.y(), (float) entry.z());
                    entry.renderState().submit(poseStack, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
                } catch (RuntimeException e) {
                    if (WARNED_BER_CLASSES.add(entry.block().getClass())) {
                        ModLog.debug("[BlueprintPreviewRenderer] SpecialModel 预览跳过 "
                                + entry.block().getDescriptionId() + ": " + e.getMessage());
                    }
                } finally {
                    poseStack.popPose();
                }
            }

            // 3. 提交 BlockEntityRenderer（模组多方块动态部分、告示牌文字、箱子等）
            if (!mesh.blockEntities.isEmpty()) {
                CameraRenderState camera = new CameraRenderState();
                camera.orientation = new Quaternionf();
                var dispatcher = mc.getBlockEntityRenderDispatcher();
                for (BlueprintPreviewMesh.BlockEntityEntry entry : mesh.blockEntities) {
                    if (selected != null && entry.block() != selected) continue;
                    poseStack.pushPose();
                    try {
                        poseStack.translate((float) entry.x(), (float) entry.y(), (float) entry.z());
                        submitBlockEntity(dispatcher, entry.entity(), poseStack, collector, camera);
                    } finally {
                        poseStack.popPose();
                    }
                }
            }

            // 4. 提交半透明网格（水体、玻璃、传送门，以及选中过滤模式下其余半透明淡化方块）
            boolean hasTranslucentSelected = mesh.translucentBatch.hasAnyMatching(selected, true);
            boolean hasDimmedOther = selected != null && (
                    mesh.solidBatch.hasAnyMatching(selected, false)
                            || mesh.cutoutBatch.hasAnyMatching(selected, false)
                            || mesh.translucentBatch.hasAnyMatching(selected, false)
            );
            if (hasTranslucentSelected || hasDimmedOther) {
                collector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(), (pose, vc) -> {
                    if (hasTranslucentSelected) {
                        mesh.translucentBatch.writeTo(pose, vc, selected, true, false);
                    }
                    if (hasDimmedOther) {
                        mesh.solidBatch.writeTo(pose, vc, selected, false, true);
                        mesh.cutoutBatch.writeTo(pose, vc, selected, false, true);
                        mesh.translucentBatch.writeTo(pose, vc, selected, false, true);
                    }
                });
            }

            // 5. 提交 3D 线框与 X/Y/Z 坐标标尺线
            boolean showSliceBox = state.singleLayerMode() || state.currentLayer() < state.structHeight() - 1;
            if (state.showBoundsAndAxes() || showSliceBox) {
                collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, vc) -> {
                    Vector3f normalScratch = new Vector3f();
                    if (state.showBoundsAndAxes()) {
                        emitBoundingBoxAndRulers(pose, vc, normalScratch, state);
                    }
                    if (showSliceBox) {
                        float sliceY = state.currentLayer() + 1.005F;
                        float pad = 0.025F;
                        emitBoxEdges(pose, vc, normalScratch,
                                -pad, sliceY, -pad,
                                state.structWidth() + pad, sliceY, state.structLength() + pad,
                                0xDDFFDD44, 1.8F, true);
                    }
                });
            }
        } finally {
            poseStack.popPose();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void submitBlockEntity(
            net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher dispatcher,
            BlockEntity entity,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        try {
            BlockEntityRenderer renderer = dispatcher.getRenderer(entity);
            if (renderer == null) return;
            BlockEntityRenderState berState = renderer.createRenderState();
            renderer.extractRenderState(entity, berState, 0.0F, Vec3.ZERO, null);
            berState.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            renderer.submit(berState, poseStack, collector, camera);
        } catch (RuntimeException e) {
            if (WARNED_BER_CLASSES.add(entity.getClass())) {
                ModLog.debug("[BlueprintPreviewRenderer] BlockEntityRenderer 预览回退跳过 "
                        + entity.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private static void emitBoundingBoxAndRulers(
            PoseStack.Pose pose,
            VertexConsumer vc,
            Vector3f normalScratch,
            State state
    ) {
        float w = state.structWidth();
        float h = state.structHeight();
        float l = state.structLength();
        float pad = 0.015F;
        emitBoxEdges(pose, vc, normalScratch, -pad, -pad, -pad, w + pad, h + pad, l + pad, 0x8800E5FF, 1.3F, false);

        float gap = Math.max(0.45F, 6.0F / Math.max(1.0F, state.scale()));
        // X 标尺 (红)
        emitRulerLine(pose, vc, normalScratch, 0.0F, 0.0F, l + gap, w, 0.0F, l + gap, 0.0F, 0.0F, 0.35F, 0xFFFF5555);
        // Z 标尺 (蓝)
        emitRulerLine(pose, vc, normalScratch, w + gap, 0.0F, 0.0F, w + gap, 0.0F, l, 0.35F, 0.0F, 0.0F, 0xFF5599FF);
        // Y 标尺 (绿)
        emitRulerLine(pose, vc, normalScratch, -gap, 0.0F, l, -gap, h, l, -0.25F, 0.0F, 0.25F, 0xFF55FF55);
    }

    private static void emitRulerLine(
            PoseStack.Pose pose,
            VertexConsumer vc,
            Vector3f normalScratch,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float tickX, float tickY, float tickZ,
            int color
    ) {
        emitLine(pose, vc, normalScratch, x0, y0, z0, x1, y1, z1, color, 1.6F);
        emitLine(pose, vc, normalScratch, x0 - tickX, y0 - tickY, z0 - tickZ, x0 + tickX, y0 + tickY, z0 + tickZ, color, 1.4F);
        emitLine(pose, vc, normalScratch, x1 - tickX, y1 - tickY, z1 - tickZ, x1 + tickX, y1 + tickY, z1 + tickZ, color, 1.4F);
    }

    private static void emitBoxEdges(
            PoseStack.Pose pose,
            VertexConsumer vc,
            Vector3f normalScratch,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int color,
            float width,
            boolean flatTopOnly
    ) {
        // 顶环（或平面四边）
        emitLine(pose, vc, normalScratch, x0, y1, z0, x1, y1, z0, color, width);
        emitLine(pose, vc, normalScratch, x1, y1, z0, x1, y1, z1, color, width);
        emitLine(pose, vc, normalScratch, x1, y1, z1, x0, y1, z1, color, width);
        emitLine(pose, vc, normalScratch, x0, y1, z1, x0, y1, z0, color, width);
        if (flatTopOnly) return;

        // 底环
        emitLine(pose, vc, normalScratch, x0, y0, z0, x1, y0, z0, color, width);
        emitLine(pose, vc, normalScratch, x1, y0, z0, x1, y0, z1, color, width);
        emitLine(pose, vc, normalScratch, x1, y0, z1, x0, y0, z1, color, width);
        emitLine(pose, vc, normalScratch, x0, y0, z1, x0, y0, z0, color, width);
        // 4 条竖边
        emitLine(pose, vc, normalScratch, x0, y0, z0, x0, y1, z0, color, width);
        emitLine(pose, vc, normalScratch, x1, y0, z0, x1, y1, z0, color, width);
        emitLine(pose, vc, normalScratch, x1, y0, z1, x1, y1, z1, color, width);
        emitLine(pose, vc, normalScratch, x0, y0, z1, x0, y1, z1, color, width);
    }

    private static void emitLine(
            PoseStack.Pose pose,
            VertexConsumer vc,
            Vector3f normalScratch,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            int color,
            float width
    ) {
        normalScratch.set(x1 - x0, y1 - y0, z1 - z0);
        if (normalScratch.lengthSquared() < 1.0E-6F) {
            normalScratch.set(0.0F, 1.0F, 0.0F);
        } else {
            normalScratch.normalize();
        }
        vc.addVertex(pose, x0, y0, z0).setColor(color).setNormal(pose, normalScratch).setLineWidth(width);
        vc.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, normalScratch).setLineWidth(width);
    }

    public record State(
            @Nullable BlueprintPreviewMesh mesh,
            int structWidth,
            int structHeight,
            int structLength,
            float yaw,
            float pitch,
            float panX,
            float panY,
            float scale,
            int currentLayer,
            boolean singleLayerMode,
            boolean showBoundsAndAxes,
            @Nullable Block selectedBlock,
            int x0,
            int y0,
            int x1,
            int y1,
            @Nullable ScreenRectangle scissorArea,
            @Nullable ScreenRectangle bounds
    ) implements PictureInPictureRenderState {
    }
}
