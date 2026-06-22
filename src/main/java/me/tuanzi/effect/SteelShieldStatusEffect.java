package me.tuanzi.effect;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModStatusEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class SteelShieldStatusEffect extends MobEffect {
    public SteelShieldStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x708090); // 钢灰色/冷钢蓝
        
        // 注册属性修改器：每级 +2 护甲，使用加法加成
        this.addAttributeModifier(Attributes.ARMOR,
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "effect.steel_shield.armor"),
            2.0, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(final ServerLevel level, final LivingEntity mob, final int amplification) {
        if (amplification > 0) {
            // 当高层数的 1 tick 效果即将过期时，我们塞入低一层 (amplification - 1) 的 100 ticks (5s) 新效果
            // 利用 MobEffectInstance.update() 逻辑，由于新效果层数较低且旧效果时间即将结束 (1 tick)，
            // 新效果会被自动挂载到隐藏效果 (hiddenEffect) 队列中。
            // 当本 tick 结束时，旧效果时间变为 0，游戏机制将自动触发 downgradeToHiddenEffect，无缝将效果降级为新效果并继续计时！
            mob.addEffect(new MobEffectInstance(ModStatusEffects.STEEL_SHIELD, 100, amplification - 1, false, false, true));
            me.tuanzi.util.ModLog.debug(mob, null, "【钢盾】层数衰减判定触发！层数由 " + (amplification + 1) + " 层衰减至 " + amplification + " 层，开启 5s 平缓防御消退。");
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(final int tickCount, final int amplification) {
        // 仅在效果最后 1 tick（即 duration == 1 时）执行 applyEffectTick，进行衰减检查和塞入
        return tickCount == 1;
    }
}
