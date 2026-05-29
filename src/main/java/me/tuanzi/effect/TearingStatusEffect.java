package me.tuanzi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class TearingStatusEffect extends MobEffect {
    public TearingStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x8A0303); // 深红色 (Tearing Red)
    }
}
