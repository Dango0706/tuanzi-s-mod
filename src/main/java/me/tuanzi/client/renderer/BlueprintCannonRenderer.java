package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.block.cannon.LaunchedItem;
import me.tuanzi.block.entity.BlueprintCannonBlockEntity;
import me.tuanzi.client.renderer.model.BlueprintCannonModel;
import me.tuanzi.client.renderer.state.BlueprintCannonRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 蓝图大炮全新运作渲染器（严格契合 Blockbench 规范与正向动力学几何）
 */
@Environment(EnvType.CLIENT)
public class BlueprintCannonRenderer implements BlockEntityRenderer<BlueprintCannonBlockEntity, BlueprintCannonRenderState> {
    public static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/block/blueprint_cannon_model.png");

    private final BlueprintCannonModel model;

    public BlueprintCannonRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new BlueprintCannonModel(context.bakeLayer(BlueprintCannonModel.LAYER_LOCATION));
    }

    /**
     * 根据大炮空间位置与俯仰/偏航/后坐力，计算炮口出膛截面正中心的世界坐标
     */
    public static Vec3 getMuzzlePosition(BlockPos cannonPos, double yaw, double pitch, double recoil) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        // 炮管转轴中心世界坐标（Pivot：位于大炮水平中心，高 0.71875 格 = 11.5 像素）
        double hingeX = cannonPos.getX() + 0.5;
        double hingeY = cannonPos.getY() + 0.71875;
        double hingeZ = cannonPos.getZ() + 0.5;

        // 炮管指向目标的单位方向向量
        double dirX = Math.sin(yawRad) * Math.cos(pitchRad);
        double dirY = Math.sin(pitchRad);
        double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);

        // 炮管出膛长度为 14 像素（0.875 格），开火后坐力使炮管向后抽退
        double length = 0.875 - recoil * 0.05 / 16.0;

        return new Vec3(
                hingeX + dirX * length,
                hingeY + dirY * length,
                hingeZ + dirZ * length
        );
    }

    /**
     * 获取炮管出射的单位朝向向量
     */
    public static Vec3 getMuzzleDirection(double yaw, double pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        return new Vec3(
                Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                Math.cos(yawRad) * Math.cos(pitchRad)
        );
    }

    /**
     * 偏航角 Yaw 与俯仰角 Pitch 解析几何解算
     */
    public static double[] getCannonAngles(BlueprintCannonBlockEntity blockEntity, BlockPos pos, float partialTicks) {
        double yaw;
        double pitch;

        BlockPos target = blockEntity.currentTarget;
        if (target != null) {
            Vec3 diff = Vec3.atLowerCornerOf(target.subtract(pos));
            if (blockEntity.previousTarget != null) {
                diff = (Vec3.atLowerCornerOf(blockEntity.previousTarget)
                        .add(Vec3.atLowerCornerOf(target.subtract(blockEntity.previousTarget))
                                .scale(partialTicks))).subtract(Vec3.atLowerCornerOf(pos));
            }

            double diffX = diff.x();
            double diffZ = diff.z();
            yaw = Mth.atan2(diffX, diffZ) / Math.PI * 180.0;

            float distance = Mth.sqrt((float) (diffX * diffX + diffZ * diffZ));
            // 真实抛物线动力学仰角：高度差 + 随距离提升的抛物线扬角补偿
            double targetPitch = Math.toDegrees(Math.atan2(diff.y() + distance * 0.35, distance));
            pitch = Mth.clamp(targetPitch, 10.0, 75.0);
        } else {
            yaw = blockEntity.defaultYaw;
            pitch = 30.0;
        }

        return new double[]{yaw, pitch};
    }

    /**
     * 出膛前 10 ticks 线性后坐力衰减判定
     */
    public static double getRecoil(BlueprintCannonBlockEntity blockEntity, float partialTicks) {
        double recoil = 0.0;
        for (LaunchedItem launched : blockEntity.flyingBlocks) {
            if (launched.ticksRemaining == 0) continue;
            if ((launched.ticksRemaining + 1 - partialTicks) > launched.totalTicks - 10) {
                recoil = Math.max(recoil, (launched.ticksRemaining + 1 - partialTicks) - launched.totalTicks + 10);
            }
        }
        return recoil;
    }

    /**
     * 空中抛物线微缩方块渲染（严格以真实炮口为弹道起始点）
     */
    private static void renderLaunchedBlocks(
            BlueprintCannonRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector
    ) {
        for (LaunchedItem launched : state.flyingBlocks) {
            if (launched.ticksRemaining == 0) continue;

            // 起始点：真实炮口正中心
            Vec3 start = getMuzzlePosition(state.cannonPos, state.yaw, state.pitch, state.recoil);
            Vec3 target = Vec3.atCenterOf(launched.target);
            Vec3 distance = target.subtract(start);

            double yDifference = target.y - start.y;
            double throwHeight = Math.max(1.0, Math.sqrt(distance.lengthSqr()) * 0.45 + yDifference * 0.4);

            float progress = ((float) launched.totalTicks - launched.ticksRemaining) / (float) launched.totalTicks;
            progress = Mth.clamp(progress, 0.0f, 1.0f);

            float t = progress;
            // 水平插值
            double currentX = start.x + (target.x - start.x) * t;
            double currentZ = start.z + (target.z - start.z) * t;
            // 贝塞尔垂直二次抛物线插值：t=0 时为 0，t=1 时为 yDifference，中间拱起 throwHeight
            double yParabola = 4.0 * (1.0 - t) * t * throwHeight + t * yDifference;
            double currentY = start.y + yParabola;

            Vec3 blockLocation = new Vec3(currentX, currentY, currentZ);

            if (launched instanceof LaunchedItem.ForBlockState forBlock && !forBlock.state.isAir()) {
                poseStack.pushPose();
                // 移至当前弹道相对于大炮的位置
                poseStack.translate(
                        blockLocation.x - state.cannonPos.getX() - 0.5,
                        blockLocation.y - state.cannonPos.getY(),
                        blockLocation.z - state.cannonPos.getZ() - 0.5
                );
                // 优雅自转
                poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(360.0f * t)).rotateX((float) Math.toRadians(360.0f * t)));
                float scale = 0.35f;
                poseStack.scale(scale, scale, scale);

                MovingBlockRenderState movingBlock = new MovingBlockRenderState();
                movingBlock.blockState = forBlock.state;
                movingBlock.blockPos = launched.target;
                movingBlock.randomSeedPos = launched.target;

                submitNodeCollector.submitMovingBlock(poseStack, movingBlock, 0);
                poseStack.popPose();
            }
        }
    }

    @Override
    public BlueprintCannonRenderState createRenderState() {
        return new BlueprintCannonRenderState();
    }

    @Override
    public void extractRenderState(
            BlueprintCannonBlockEntity blockEntity,
            BlueprintCannonRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        BlockPos pos = blockEntity.getBlockPos();
        state.cannonPos = pos;

        // 1. 解算偏航角与俯仰角
        double[] angles = getCannonAngles(blockEntity, pos, partialTicks);
        state.yaw = angles[0];
        state.pitch = angles[1];

        // 2. 解算液压后坐力
        state.recoil = getRecoil(blockEntity, partialTicks);

        // 3. 拷贝飞行方块状态
        state.flyingBlocks.clear();
        state.flyingBlocks.addAll(blockEntity.flyingBlocks);
    }

    @Override
    public void submit(
            BlueprintCannonRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera
    ) {
        poseStack.pushPose();
        // 移至大炮中心底座 (Pivot 位于方块中心地基 Y=0)
        poseStack.translate(0.5, 0.0, 0.5);

        // === 1. 渲染固定底座 base（不随偏航角旋转，永远稳稳坐落于地面） ===
        submitNodeCollector.submitModelPart(
                this.model.base,
                poseStack,
                this.model.renderType(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                null
        );

        // === 2. 渲染回转机身 turret 与俯仰炮管 barrel ===
        poseStack.pushPose();
        // 水平偏航角旋转（JOML 绕 +Y 轴旋转：+state.yaw 使 +Z 炮口精准朝向目标方位）
        poseStack.mulPose(new org.joml.Matrix4f().rotationY((float) Math.toRadians(state.yaw)));

        // 姿态调整：炮口昂首指向目标仰角（负 X 轴旋转使得 +Z 炮口向上扬起）
        this.model.turret.yRot = 0.0f;
        this.model.barrel.xRot = (float) Math.toRadians(-state.pitch);
        // 后坐力向后抽退（炮口在 +Z，后坐力沿轴线向 -Z 抽动）
        this.model.barrel.z = (float) (-state.recoil * 0.05);

        submitNodeCollector.submitModelPart(
                this.model.turret,
                poseStack,
                this.model.renderType(TEXTURE),
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                null
        );
        poseStack.popPose();
        poseStack.popPose();

        // === 3. 渲染空中飞行微缩方块（严格从真实炮口射出） ===
        if (!state.flyingBlocks.isEmpty()) {
            renderLaunchedBlocks(state, poseStack, submitNodeCollector);
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
