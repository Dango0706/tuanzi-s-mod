package me.tuanzi.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import me.tuanzi.client.renderer.model.CinderMeteorModel;
import me.tuanzi.client.renderer.state.CinderMeteorRenderState;
import me.tuanzi.entity.CinderMeteorEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class CinderMeteorRenderer extends GeoEntityRenderer<CinderMeteorEntity, CinderMeteorRenderState> {
    public CinderMeteorRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderMeteorModel());
        this.shadowRadius = 1.2F;
        this.withScale(3.5F);
    }

    @Override
    protected int getBlockLightLevel(CinderMeteorEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public CinderMeteorRenderState createRenderState(CinderMeteorEntity entity, Void unused) {
        return new CinderMeteorRenderState();
    }

    @Override
    public void extractRenderState(CinderMeteorEntity entity, CinderMeteorRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }
}
