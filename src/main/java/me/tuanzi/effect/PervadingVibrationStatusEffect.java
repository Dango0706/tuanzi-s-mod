package me.tuanzi.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * 贯震状态效果
 * 附魔【贯震铭刻】专属状态。
 * 冷却 >= 80% 攻击命中直接获得 5 层 (amplifier = 4)；冷却 < 80% 命中后获得 1 层。
 * 最高 5 层，持续 4 秒 (80 ticks)。具体增伤在 DamageCalculator 中统一计算。
 */
public class PervadingVibrationStatusEffect extends MobEffect {
    public PervadingVibrationStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xCC7722); // 贯震琥珀铜色
    }
}
