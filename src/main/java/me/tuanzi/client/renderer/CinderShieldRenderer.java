package me.tuanzi.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import me.tuanzi.client.renderer.model.CinderShieldModel;
import me.tuanzi.client.renderer.state.CinderShieldRenderState;
import me.tuanzi.entity.CinderShieldPartEntity;
import me.tuanzi.entity.CinderSovereignEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class CinderShieldRenderer extends GeoEntityRenderer<CinderShieldPartEntity, CinderShieldRenderState> {
    public CinderShieldRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderShieldModel());
        this.shadowRadius = 0.6F;
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public CinderShieldRenderState createRenderState(CinderShieldPartEntity entity, Void unused) {
        return new CinderShieldRenderState();
    }

    @Override
    public void extractRenderState(CinderShieldPartEntity entity, CinderShieldRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        CinderSovereignEntity boss = entity.getParentBoss();
        state.isPhase2 = (boss != null && boss.isPhase2());
        state.health = entity.getShieldHealth();
    }
}
