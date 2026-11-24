package me.tuanzi.effect;

import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.ModLog;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * 状态效果「净化脉冲」：
 * 1. 施加瞬间清除所有牛奶可清除的负面状态（保留正面/中性及「肾上腺素透支」）；
 * 2. 持续期间每 5 秒（100 ticks）自动清除 1 个当前牛奶可清除的负面状态；
 * 3. 持续期间新获得的牛奶可清除的负面状态持续时间 -50%；
 * 4. 不清除「肾上腺素透支」与 Boss 过热条等非状态效果机制。
 */
public class PurifyingPulseStatusEffect extends MobEffect {
    public static final int CLEAR_INTERVAL_TICKS = 100; // 5 秒

    public PurifyingPulseStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x5CE6E6); // 清澈青蓝色
    }

    /**
     * 判断给定状态效果实例是否属于“牛奶可清除的负面状态”。
     */
    public static boolean isMilkClearableHarmful(MobEffectInstance instance) {
        return instance != null && isMilkClearableHarmful(instance.getEffect());
    }

    /**
     * 判断给定状态效果是否属于“牛奶可清除的负面状态”。
     * 排除正面/中性效果以及不可被牛奶清除的「肾上腺素透支」。
     */
    public static boolean isMilkClearableHarmful(Holder<MobEffect> effect) {
        if (effect == null) {
            return false;
        }
        if (effect.equals(ModStatusEffects.ADRENALINE_OVERDRAW)) {
            return false;
        }
        return effect.value().getCategory() == MobEffectCategory.HARMFUL;
    }

    /**
     * 计算净化脉冲期间新获得负面状态的减半持续时间（至少 1 tick，永久效果 -1 不变）。
     */
    public static int halveDuration(int duration) {
        if (duration == MobEffectInstance.INFINITE_DURATION || duration <= 0) {
            return duration;
        }
        return Math.max(1, Math.round(duration * 0.5f));
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) {
            return;
        }
        List<Holder<MobEffect>> toRemove = new ArrayList<>();
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (isMilkClearableHarmful(instance)) {
                toRemove.add(instance.getEffect());
            }
        }
        for (Holder<MobEffect> effect : toRemove) {
            entity.removeEffect(effect);
        }
        ModLog.debug(entity, null, "【净化脉冲】生效瞬间清除了 " + toRemove.size() + " 个可净化负面状态。");
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return tickCount % CLEAR_INTERVAL_TICKS == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Holder<MobEffect> target = null;
        for (MobEffectInstance instance : entity.getActiveEffects()) {
            if (isMilkClearableHarmful(instance)) {
                target = instance.getEffect();
                break;
            }
        }
        if (target != null) {
            entity.removeEffect(target);
            ModLog.debug(entity, null, "【净化脉冲】周期净化：已自动清除负面状态 " + target.value().getDescriptionId());
        }
        return true;
    }
}
