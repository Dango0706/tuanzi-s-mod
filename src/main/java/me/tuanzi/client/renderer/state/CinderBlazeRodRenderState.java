package me.tuanzi.client.renderer.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Environment(EnvType.CLIENT)
public class CinderBlazeRodRenderState extends EntityRenderState {
    public float health = 15.0F;
}
