package me.tuanzi.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.entity.player.PlayerModelType;
import me.tuanzi.entity.DecoyEntity;
import me.tuanzi.client.entity.ClientDecoyEntity;

@Environment(EnvType.CLIENT)
public class DecoyRenderer extends EntityRenderer<ClientDecoyEntity, AvatarRenderState> {
    public static PlayerSkinRenderCache skinRenderCache;

    private final AvatarRenderer<ClientDecoyEntity> wideRenderer;
    private final AvatarRenderer<ClientDecoyEntity> slimRenderer;

    public DecoyRenderer(EntityRendererProvider.Context context) {
        super(context);
        skinRenderCache = context.getPlayerSkinRenderCache();
        this.wideRenderer = new AvatarRenderer<>(context, false);
        this.slimRenderer = new AvatarRenderer<>(context, true);
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new AvatarRenderState();
    }

    @Override
    public void extractRenderState(ClientDecoyEntity entity, AvatarRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.getSkin().model() == PlayerModelType.SLIM) {
            this.slimRenderer.extractRenderState(entity, state, partialTicks);
        } else {
            this.wideRenderer.extractRenderState(entity, state, partialTicks);
        }
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);
        if (state.skin.model() == PlayerModelType.SLIM) {
            this.slimRenderer.submit(state, poseStack, submitNodeCollector, camera);
        } else {
            this.wideRenderer.submit(state, poseStack, submitNodeCollector, camera);
        }
    }
}
