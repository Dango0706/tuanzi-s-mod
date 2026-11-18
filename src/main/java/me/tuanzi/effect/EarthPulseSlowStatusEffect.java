package me.tuanzi.effect;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 地脉迟滞状态效果
 * 定桩重击震荡波命中敌对目标后施加，持续 1.5 秒 (30 ticks)。
 * 普通目标 (amplifier = 1) 减速 20%，Boss 目标 (amplifier = 0) 减半减速 10%。
 */
public class EarthPulseSlowStatusEffect extends MobEffect {
    public static final Identifier SLOW_MODIFIER_ID = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "effect.earth_pulse_slow.movement_speed");

    public EarthPulseSlowStatusEffect() {
        super(MobEffectCategory.HARMFUL, 0x554433); // 地脉深岩泥褐色

        // 基准 amount 为 -0.10：
        // amplifier 0 (Boss) 时为 -0.10 * 1 = -0.10 (-10%)
        // amplifier 1 (普通目标) 时为 -0.10 * 2 = -0.20 (-20%)
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                SLOW_MODIFIER_ID,
                -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
