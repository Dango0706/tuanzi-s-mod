package me.tuanzi.client.renderer.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.entity.CinderFireballEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class CinderFireballModel extends DefaultedEntityGeoModel<CinderFireballEntity> {
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "entity/cinder_fireball");
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/entity/cinder_fireball.png");
    private static final Identifier ANIMATION = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "entity/cinder_fireball");

    public CinderFireballModel() {
        super(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_fireball"));
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
    public Identifier getAnimationResource(CinderFireballEntity animatable) {
        return ANIMATION;
    }
}
