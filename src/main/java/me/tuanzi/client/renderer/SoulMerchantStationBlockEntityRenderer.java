package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import me.tuanzi.block.entity.SoulMerchantStationBlockEntity;
import me.tuanzi.client.renderer.state.SoulMerchantStationRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;

public class SoulMerchantStationBlockEntityRenderer implements BlockEntityRenderer<SoulMerchantStationBlockEntity, SoulMerchantStationRenderState> {
    public SoulMerchantStationBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public SoulMerchantStationRenderState createRenderState() {
        return new SoulMerchantStationRenderState();
    }

    @Override
    public void extractRenderState(
        final SoulMerchantStationBlockEntity blockEntity,
        final SoulMerchantStationRenderState state,
        final float partialTicks,
        final Vec3 cameraPosition,
        final ModelFeatureRenderer.@org.jspecify.annotations.Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.hasVillager = blockEntity.hasVillager();
    }

    @Override
    public void submit(
        final SoulMerchantStationRenderState state,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final CameraRenderState camera
    ) {
        // 缩微村民作为真实的 noAI 实体现已在世界中由引擎高保真渲染，此处无需任何冗余提交绘制
    }
}
