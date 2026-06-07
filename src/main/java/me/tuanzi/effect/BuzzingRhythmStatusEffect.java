package me.tuanzi.effect;

import me.tuanzi.Tuanzis_mod;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class BuzzingRhythmStatusEffect extends MobEffect {
    public BuzzingRhythmStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFD700); // 金黄色，蜂鸣节律颜色
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "effect.buzzing_rhythm.attack_damage"), 
            0.03, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
