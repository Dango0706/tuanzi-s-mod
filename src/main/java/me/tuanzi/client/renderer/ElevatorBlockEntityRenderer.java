package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.tuanzi.block.entity.ElevatorBlockEntity;
import me.tuanzi.client.renderer.state.ElevatorRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 电梯伪装渲染器：当电梯处于伪装状态时，以原生 MovingBlockRenderState 方式渲染目标方块外观。
 */
@Environment(EnvType.CLIENT)
public class ElevatorBlockEntityRenderer implements BlockEntityRenderer<ElevatorBlockEntity, ElevatorRenderState> {
    public ElevatorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public ElevatorRenderState createRenderState() {
        return new ElevatorRenderState();
    }

    @Override
    public void extractRenderState(
            final ElevatorBlockEntity blockEntity,
            final ElevatorRenderState state,
            final float partialTicks,
            final Vec3 cameraPosition,
            final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.movingBlock = null;
        if (!blockEntity.hasDisguise()) {
            return;
        }
        BlockState disguiseState = blockEntity.getDisguiseState();
        if (disguiseState == null || disguiseState.getRenderShape() != RenderShape.MODEL) {
            return;
        }
        if (blockEntity.getLevel() instanceof ClientLevel clientLevel) {
            BlockPos pos = blockEntity.getBlockPos();
            MovingBlockRenderState movingBlock = new MovingBlockRenderState();
            movingBlock.randomSeedPos = pos;
            movingBlock.blockPos = pos;
            movingBlock.blockState = disguiseState;
            movingBlock.biome = clientLevel.getBiome(pos);
            movingBlock.cardinalLighting = clientLevel.cardinalLighting();
            movingBlock.lightEngine = clientLevel.getLightEngine();
            state.movingBlock = movingBlock;
        }
    }

    @Override
    public void submit(
            final ElevatorRenderState state,
            final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final CameraRenderState camera
    ) {
        if (state.movingBlock != null) {
            submitNodeCollector.submitMovingBlock(poseStack, state.movingBlock, 0);
        }
    }
}
