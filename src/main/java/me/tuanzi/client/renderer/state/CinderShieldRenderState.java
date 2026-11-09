package me.tuanzi.client.renderer.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Environment(EnvType.CLIENT)
public class CinderShieldRenderState extends EntityRenderState {
    public boolean isPhase2 = false;
    public float health = 35.0F;
}
