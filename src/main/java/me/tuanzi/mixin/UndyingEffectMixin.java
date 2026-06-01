package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class UndyingEffectMixin {

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$useUndyingEffect(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity entity = (LivingEntity) (Object) this;
        
        if (entity.hasEffect(ModStatusEffects.UNDYING)) {
            // 检查是否是玩家或已驯服生物 (拥有者不为空)
            boolean isPlayer = entity instanceof Player;
            boolean isTamed = entity instanceof TamableAnimal tamed && tamed.isTame();

            if (!isPlayer && !isTamed) {
                return;
            }

            // 像不死图腾一样处理
            entity.setHealth(1.0F);
            entity.removeAllEffects();
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));
            entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));
            
            // 触发图腾颗粒和声音
            entity.level().broadcastEntityEvent(entity, (byte) 35);

            me.tuanzi.util.ModLog.debug(entity, null, "不死药水效果触发成功：免除致命伤害，生命值设为 1.0F 并清除所有状态效果，已获得生命恢复 II、防火 I 和伤害吸收 II 效果。");

            // 移除不死状态效果
            entity.removeEffect(ModStatusEffects.UNDYING);

            cir.setReturnValue(true);
        }
    }
}
