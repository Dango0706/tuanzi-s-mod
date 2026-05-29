package me.tuanzi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class AdrenalineOverdrawStatusEffect extends MobEffect {
    public AdrenalineOverdrawStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000); // 深暗红色
    }
}
