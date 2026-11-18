package me.tuanzi.effect;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * 地脉回响状态效果
 * 定桩重击命中后在 2 秒内静止不动获得，锁定保留 5 秒。
 * 下一次近战攻击的冷却恢复速度 +10% (即攻击速度 +10%)，在下一次攻击动作开始时消耗。
 */
public class EarthEchoStatusEffect extends MobEffect {
    public static final Identifier ECHO_SPEED_MODIFIER_ID = Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "effect.earth_echo.attack_speed");

    public EarthEchoStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xDDAA22); // 地脉星核金光

        this.addAttributeModifier(Attributes.ATTACK_SPEED,
                ECHO_SPEED_MODIFIER_ID,
                0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}
