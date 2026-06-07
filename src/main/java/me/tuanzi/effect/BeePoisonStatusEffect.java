package me.tuanzi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BeePoisonStatusEffect extends MobEffect {
    public BeePoisonStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0xE6B800); // 蜜蜂毒素黄色
    }
}
