package me.tuanzi.client.renderer;

import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import me.tuanzi.client.renderer.model.CinderFireballModel;
import me.tuanzi.client.renderer.state.CinderFireballRenderState;
import me.tuanzi.entity.CinderFireballEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class CinderFireballRenderer extends GeoEntityRenderer<CinderFireballEntity, CinderFireballRenderState> {
    public CinderFireballRenderer(EntityRendererProvider.Context context) {
        super(context, new CinderFireballModel());
        this.shadowRadius = 0.5F;
        this.withScale(2.5F);
    }

    @Override
    protected int getBlockLightLevel(CinderFireballEntity entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public CinderFireballRenderState createRenderState(CinderFireballEntity entity, Void unused) {
        return new CinderFireballRenderState();
    }

    @Override
    public void extractRenderState(CinderFireballEntity entity, CinderFireballRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
    }
}
