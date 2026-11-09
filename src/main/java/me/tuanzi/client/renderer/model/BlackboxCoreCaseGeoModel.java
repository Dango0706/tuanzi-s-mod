package me.tuanzi.client.renderer.model;

import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.block.entity.BlackboxCoreCaseBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class BlackboxCoreCaseGeoModel extends DefaultedBlockGeoModel<BlackboxCoreCaseBlockEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "block/blackbox_core_case");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "block/blackbox_core_case");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/block/blackbox_core_case.png");

    public BlackboxCoreCaseGeoModel() {
        super(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "blackbox_core_case"));
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getAnimationResource(BlackboxCoreCaseBlockEntity animatable) {
        return ANIMATION;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }
}
