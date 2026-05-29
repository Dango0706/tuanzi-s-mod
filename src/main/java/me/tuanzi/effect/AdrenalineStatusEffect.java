package me.tuanzi.effect;

import me.tuanzi.Tuanzis_mod;
import me.tuanzi.init.ModStatusEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class AdrenalineStatusEffect extends MobEffect {
    public AdrenalineStatusEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE25822); // 橙红色
        this.addAttributeModifier(Attributes.ATTACK_SPEED, 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "effect.adrenaline.attack_speed"), 
            0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, 
            Identifier.fromNamespaceAndPath(Tuanzis_mod.MOD_ID, "effect.adrenaline.movement_speed"), 
            0.075, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public void onEffectStarted(LivingEntity entity, int amplifier) {
        if (entity.level().isClientSide()) return;

        MobEffectInstance adrenaline = entity.getEffect(ModStatusEffects.ADRENALINE);
        int duration = adrenaline != null ? adrenaline.getDuration() : 900;

        MobEffectInstance oldOverdraw = entity.getEffect(ModStatusEffects.ADRENALINE_OVERDRAW);
        if (oldOverdraw == null) {
            // 第一次获得效果，施加肾上腺素透支效果（持续时间为药水时间 + 2秒缓冲以确保Buff结束时透支效果依旧存在）
            entity.addEffect(new MobEffectInstance(ModStatusEffects.ADRENALINE_OVERDRAW, duration + 40, amplifier, false, true, true));
        } else {
            // 连续饮用，叠加透支效果的等级与时间。最高可叠加至 IV 级（amplifier = 3）
            int newAmplifier = Math.min(3, oldOverdraw.getAmplifier() + 1);
            int newDuration = oldOverdraw.getDuration() + duration;
            entity.addEffect(new MobEffectInstance(ModStatusEffects.ADRENALINE_OVERDRAW, newDuration, newAmplifier, false, true, true));
        }
    }
}
