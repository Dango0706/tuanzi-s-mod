package me.tuanzi.effect;

import me.tuanzi.util.HydrophobiaDamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public class HydrophobiaStatusEffect extends MobEffect {
    public HydrophobiaStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x00008B); // 深蓝色
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
        if (mob.isInWater()) {
            var typeHolder = mob.registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.GENERIC);
            HydrophobiaDamageSource src = new HydrophobiaDamageSource(typeHolder);

            // 绝对扣减生命值，以绝对无视护甲与任何效果减免
            float newHealth = mob.getHealth() - 2.0F;
            mob.setHealth(Math.max(0.0F, newHealth));
            if (newHealth <= 0.0F) {
                mob.die(src);
            }
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
        // 每秒受到伤害，1秒 = 20 ticks
        return tickCount % 20 == 0;
    }
}
