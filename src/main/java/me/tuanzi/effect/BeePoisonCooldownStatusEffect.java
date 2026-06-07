package me.tuanzi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

public class BeePoisonCooldownStatusEffect extends MobEffect {
    public BeePoisonCooldownStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x5A5A5A); // 冷却灰色
    }
}
