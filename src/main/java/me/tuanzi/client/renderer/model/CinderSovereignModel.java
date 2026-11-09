package me.tuanzi.client.renderer.model;

import com.geckolib.model.DefaultedEntityGeoModel;
import me.tuanzi.Tuanzis_mod;
import me.tuanzi.entity.CinderSovereignEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class CinderSovereignModel extends DefaultedEntityGeoModel<CinderSovereignEntity> {
    public CinderSovereignModel() {
        super(Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "cinder_sovereign"));
    }
}

