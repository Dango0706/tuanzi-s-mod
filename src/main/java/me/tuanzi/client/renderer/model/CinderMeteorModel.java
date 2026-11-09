package me.tuanzi.client.renderer.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.entity.CinderMeteorEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class CinderMeteorModel extends DefaultedEntityGeoModel<CinderMeteorEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "entity/cinder_meteor");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/entity/cinder_meteor.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "entity/cinder_meteor");

    public CinderMeteorModel() {
        super(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_meteor"));
    }

    @Override
    public Identifier getModelResource(GeoRenderState state) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }

    @Override
    public Identifier getAnimationResource(CinderMeteorEntity animatable) {
        return ANIMATION;
    }
}
