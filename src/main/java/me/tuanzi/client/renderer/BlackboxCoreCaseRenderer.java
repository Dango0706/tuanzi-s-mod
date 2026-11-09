package me.tuanzi.client.renderer;

import com.geckolib.renderer.GeoBlockRenderer;
import me.tuanzi.block.entity.BlackboxCoreCaseBlockEntity;
import me.tuanzi.client.renderer.model.BlackboxCoreCaseGeoModel;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

@Environment(EnvType.CLIENT)
public class BlackboxCoreCaseRenderer extends GeoBlockRenderer<BlackboxCoreCaseBlockEntity, BlockEntityRenderState> {
    public BlackboxCoreCaseRenderer(BlockEntityRendererProvider.Context context) {
        super(context, new BlackboxCoreCaseGeoModel());
    }
}
