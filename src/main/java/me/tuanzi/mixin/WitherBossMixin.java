package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin extends Monster {

    protected WitherBossMixin(EntityType<? extends Monster> entityType, Level level) {
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
            me.tuanzi.util.ModLog.debug(source, this, "【凋零效果免疫解除】允许对凋零施加效果: " + effect.getRegisteredName());
            cir.setReturnValue(super.addEffect(newEffect, source));
        }
    }
}
