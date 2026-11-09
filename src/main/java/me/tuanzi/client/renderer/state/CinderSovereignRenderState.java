package me.tuanzi.client.renderer.state;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

@Environment(EnvType.CLIENT)
public class CinderSovereignRenderState extends LivingEntityRenderState {
    public int shieldBitmask = 0b1111;
    public int rodBitmask = 0b111111111111;

    public boolean isShieldAlive(int index) {
        return (shieldBitmask & (1 << index)) != 0;
    }

    public boolean isRodAlive(int index) {
        return (rodBitmask & (1 << index)) != 0;
    }
}
