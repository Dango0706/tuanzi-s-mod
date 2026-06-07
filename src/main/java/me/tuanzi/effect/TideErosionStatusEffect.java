package me.tuanzi.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class TideErosionStatusEffect extends MobEffect {
    public TideErosionStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x40E0D0); // 碧绿/浅海碧色，代表海洋能量的侵蚀
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
        mob.hurt(mob.damageSources().magic(), 1.0f);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
        // 每秒受到伤害，1秒 = 20 ticks
        return tickCount % 20 == 0;
    }
}
