package me.tuanzi.client.renderer.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.entity.CinderShieldPartEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class CinderShieldModel extends DefaultedEntityGeoModel<CinderShieldPartEntity> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/entity/cinder_sovereign.png");

    public CinderShieldModel() {
        super(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_shield"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }
}
