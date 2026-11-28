package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.tuanzi.block.PrintedModelBlock;
import me.tuanzi.block.PrintedModelMountMode;
import me.tuanzi.block.entity.PrintedModelBlockEntity;
import me.tuanzi.client.canvas.ClientCanvasDataCache;
import me.tuanzi.client.canvas.PrintedModelChunkedMesh;
import me.tuanzi.client.renderer.state.PrintedModelRenderState;
import me.tuanzi.config.ModConfig;
import me.tuanzi.world.canvas.CanvasMetadata;
import me.tuanzi.world.canvas.CanvasType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 打印模型方块实体渲染器：
 * 重放预烘焙的 16³ 静态网格并支持距离 LOD 与最大视距剔除，零逐帧重烘焙开销。
 */
@Environment(EnvType.CLIENT)
public class PrintedModelBlockEntityRenderer implements BlockEntityRenderer<PrintedModelBlockEntity, PrintedModelRenderState> {
    public PrintedModelBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public PrintedModelRenderState createRenderState() {
        return new PrintedModelRenderState();
    }

    @Override
    public void extractRenderState(
            PrintedModelBlockEntity blockEntity,
            PrintedModelRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        CanvasMetadata meta = blockEntity.getMetadata();
        state.mesh = null;
        state.skipRender = true;
        if (meta.uuid() == null) {
            return;
        }

        double distSqr = cameraPosition.distanceToSqr(Vec3.atCenterOf(blockEntity.getBlockPos()));
        int maxDist = ModConfig.getPrintedModelMaxRenderDistance();
        if (distSqr > (double) maxDist * maxDist) {
            return;
        }

        PrintedModelChunkedMesh mesh = ClientCanvasDataCache.getOrBuildMesh(Minecraft.getInstance(), meta.uuid(), meta.dataHash());
        if (mesh == null) {
            return;
        }

        BlockState blockState = blockEntity.getBlockState();
        int lodDist = ModConfig.getPrintedModelLodDistance();
        state.mesh = mesh;
        state.sizeX = meta.size();
        state.sizeZ = meta.size();
        state.sizeY = meta.type().getHeightForSize(meta.size());
        state.printScale = Math.max(1, meta.printScale());
        state.canvasType = meta.type();
        state.facing = blockState.hasProperty(PrintedModelBlock.FACING) ? blockState.getValue(PrintedModelBlock.FACING) : Direction.NORTH;
        state.mountMode = blockState.hasProperty(PrintedModelBlock.MOUNT_MODE) ? blockState.getValue(PrintedModelBlock.MOUNT_MODE) : PrintedModelMountMode.FLOOR;
        state.useLod = distSqr > (double) lodDist * lodDist;
        state.skipRender = false;
    }

    @Override
    public void submit(
            PrintedModelRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        if (state.skipRender || state.mesh == null) {
            return;
        }
        PrintedModelChunkedMesh mesh = state.mesh;
        int worldLight = state.lightCoords;
        boolean useLod = state.useLod;

        poseStack.pushPose();
        applyTransform(poseStack, state);

        if (useLod || mesh.hasSolid()) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.solidMovingBlock(),
                    (pose, vc) -> mesh.emitLayer(ChunkSectionLayer.SOLID, pose, vc, worldLight, useLod));
        }
        if (useLod || mesh.hasCutout()) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.cutoutMovingBlock(),
                    (pose, vc) -> mesh.emitLayer(ChunkSectionLayer.CUTOUT, pose, vc, worldLight, useLod));
        }
        if (!useLod && mesh.hasTranslucent()) {
            submitNodeCollector.submitCustomGeometry(poseStack, RenderTypes.translucentMovingBlock(),
                    (pose, vc) -> mesh.emitLayer(ChunkSectionLayer.TRANSLUCENT, pose, vc, worldLight, false));
        }

        poseStack.popPose();
    }

    private static void applyTransform(PoseStack poseStack, PrintedModelRenderState state) {
        float scale = state.printScale;
        boolean is2d = state.canvasType != CanvasType.VOXEL_3D;
        float sx = scale / Math.max(1, state.sizeX);
        float sy = is2d ? (1.0f / 16.0f) : (scale / Math.max(1, state.sizeY));
        float sz = scale / Math.max(1, state.sizeZ);

        if (is2d && state.mountMode == PrintedModelMountMode.WALL) {
            switch (state.facing) {
                case SOUTH -> {
                    // 贴北墙朝南：原 X->X, 原 Y(厚度)->Z(0..1/16), 原 Z(图像竖轴)->(scale - Y)
                    poseStack.translate(0.0f, scale, 0.0f);
                    poseStack.rotate(Axis.XP.rotationDegrees(90.0f));
                }
                case NORTH -> {
                    poseStack.translate(scale, scale, 1.0f);
                    poseStack.rotate(Axis.YP.rotationDegrees(180.0f));
                    poseStack.rotate(Axis.XP.rotationDegrees(90.0f));
                }
                case EAST -> {
                    poseStack.translate(0.0f, scale, scale);
                    poseStack.rotate(Axis.YP.rotationDegrees(90.0f));
                    poseStack.rotate(Axis.XP.rotationDegrees(90.0f));
                }
                case WEST -> {
                    poseStack.translate(1.0f, scale, 0.0f);
                    poseStack.rotate(Axis.YP.rotationDegrees(-90.0f));
                    poseStack.rotate(Axis.XP.rotationDegrees(90.0f));
                }
                default -> {
                }
            }
            poseStack.scale(sx, sy, sz);
            return;
        }

        // FLOOR / CEILING 水平旋转保持在 [0..scale, 0..scale] 正向占用区域内
        switch (state.facing) {
            case EAST -> {
                poseStack.translate(scale, 0.0f, 0.0f);
                poseStack.rotate(Axis.YP.rotationDegrees(-90.0f));
            }
            case SOUTH -> {
                poseStack.translate(scale, 0.0f, scale);
                poseStack.rotate(Axis.YP.rotationDegrees(180.0f));
            }
            case WEST -> {
                poseStack.translate(0.0f, 0.0f, scale);
                poseStack.rotate(Axis.YP.rotationDegrees(90.0f));
            }
            default -> {
            }
        }
        if (is2d && state.mountMode == PrintedModelMountMode.CEILING) {
            poseStack.translate(0.0f, 15.0f / 16.0f, 0.0f);
        }
        poseStack.scale(sx, sy, sz);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return ModConfig.getPrintedModelMaxRenderDistance();
    }
}
