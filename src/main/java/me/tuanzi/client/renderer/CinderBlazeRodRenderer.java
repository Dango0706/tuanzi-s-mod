package me.tuanzi.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import me.tuanzi.client.renderer.model.CinderBlazeRodModel;
import me.tuanzi.client.renderer.state.CinderBlazeRodRenderState;
import me.tuanzi.entity.CinderBlazeRodPartEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

@Environment(EnvType.CLIENT)
public class CinderBlazeRodRenderer extends GeoEntityRenderer<CinderBlazeRodPartEntity, CinderBlazeRodRenderState> {
    public CinderBlazeRodRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderBlazeRodModel());
        this.shadowRadius = 0.3F;
        this.withRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public CinderBlazeRodRenderState createRenderState(CinderBlazeRodPartEntity entity, Void unused) {
        return new CinderBlazeRodRenderState();
    }

    @Override
    public void extractRenderState(CinderBlazeRodPartEntity entity, CinderBlazeRodRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.health = entity.getRodHealth();
    }
}
