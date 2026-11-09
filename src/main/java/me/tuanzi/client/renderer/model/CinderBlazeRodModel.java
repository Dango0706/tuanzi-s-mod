package me.tuanzi.client.renderer.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.base.GeoRenderState;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.entity.CinderBlazeRodPartEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class CinderBlazeRodModel extends DefaultedEntityGeoModel<CinderBlazeRodPartEntity> {
    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "textures/entity/cinder_sovereign.png");

    public CinderBlazeRodModel() {
        super(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_blaze_rod"));
    }

    @Override
    public Identifier getTextureResource(GeoRenderState state) {
        return TEXTURE;
    }
}
