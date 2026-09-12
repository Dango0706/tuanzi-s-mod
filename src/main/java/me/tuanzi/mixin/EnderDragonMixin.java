package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin extends Mob {

    @Shadow
    protected abstract void reallyHurt(ServerLevel level, DamageSource source, float damage);

    protected EnderDragonMixin(EntityType<? extends Mob> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "addEffect", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$allowModStatusEffects(MobEffectInstance newEffect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        Holder<MobEffect> effect = newEffect.getEffect();
        if (effect.is(ModStatusEffects.TEARING)
            || effect.is(ModStatusEffects.BEE_POISON)
            || effect.is(ModStatusEffects.BEE_POISON_COOLDOWN)
            || effect.is(ModStatusEffects.RESONANCE)
            || effect.is(ModStatusEffects.TIDE_EROSION)) {
            me.tuanzi.util.ModLog.debug(source, this, "【末影龙效果免疫解除】允许对末影龙施加效果: " + effect.getRegisteredName());
            cir.setReturnValue(super.addEffect(newEffect, source));
        }
    }

    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$allowModEffectDamage(ServerLevel level, DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (source.getEntity() == null && source.is(DamageTypes.MAGIC)
            && (this.hasEffect(ModStatusEffects.TEARING) || this.hasEffect(ModStatusEffects.TIDE_EROSION))) {
            me.tuanzi.util.ModLog.debug(null, this, "【末影龙效果伤害生效】允许撕裂/潮汐侵蚀魔法伤害对末影龙扣血: " + damage);
            this.reallyHurt(level, source, damage);
            cir.setReturnValue(true);
        }
    }
}
