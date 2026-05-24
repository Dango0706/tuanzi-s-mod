package me.tuanzi.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;

@Environment(EnvType.CLIENT)
public class ScarecrowRenderState extends ArmorStandRenderState {
    public final MovingBlockRenderState movingBlockRenderState = new MovingBlockRenderState();
}
