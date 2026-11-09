package me.tuanzi.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import me.tuanzi.client.renderer.model.CinderSovereignModel;
import me.tuanzi.client.renderer.state.CinderSovereignRenderState;
import me.tuanzi.entity.CinderSovereignEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class CinderSovereignRenderer extends GeoEntityRenderer<CinderSovereignEntity, CinderSovereignRenderState> {
    public CinderSovereignRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderSovereignModel());
        this.shadowRadius = 1.4F;
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public CinderSovereignRenderState createRenderState(CinderSovereignEntity entity, Void unused) {
        return new CinderSovereignRenderState();
    }

    @Override
    public void extractRenderState(CinderSovereignEntity entity, CinderSovereignRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.shieldBitmask = entity.getShieldBitmask();
        state.rodBitmask = entity.getRodBitmask();
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<CinderSovereignRenderState> renderPassInfo, BoneSnapshots boneSnapshots) {
        super.adjustModelBonesForRender(renderPassInfo, boneSnapshots);
    }
}
