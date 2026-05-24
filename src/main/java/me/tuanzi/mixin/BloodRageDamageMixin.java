package me.tuanzi.mixin;

import me.tuanzi.util.DamageCalculator;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LivingEntity.class)
public abstract class BloodRageDamageMixin {

    @ModifyVariable(
        method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
        at = @At("HEAD"),
        argsOnly = true
    )
    private float tuanzis_mod$modifyHurtAmount(float amount, ServerLevel level, DamageSource source) {
        LivingEntity target = (LivingEntity) (Object) this;
        return DamageCalculator.calculateDamage(amount, source, target);
    }
}
