package me.tuanzi.effect;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class StarRiftStatusEffect extends MobEffect {
    public StarRiftStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x6C5CE7); // 靛紫色星辰色调
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        // 每 10 刻产生微弱星隙粒子
        if (mob.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.PORTAL,
                    mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(),
                    2, 0.2, 0.2, 0.2, 0.05);
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }
}
