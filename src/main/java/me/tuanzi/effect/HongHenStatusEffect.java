package me.tuanzi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class HongHenStatusEffect extends MobEffect {
    public HongHenStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0xFF2222); // 鲜红色标记 (Bright Red)
    }
}
