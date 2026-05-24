package me.tuanzi.mixin;

import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.BloodRageDamageSource;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BloodRageEffectMixin {
    @Unique
    private boolean tuanzis_mod$hadBloodRageEffect = false;

    @Unique
    private boolean tuanzis_mod$shouldApplyBloodRageFeedback = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void tuanzis_mod$tickBloodRageFeedback(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity instanceof Player player) {
            MobEffectInstance effect = player.getEffect(ModStatusEffects.BLOOD_RAGE);
            boolean hasEffect = effect != null;

            if (hasEffect) {
                int duration = effect.getDuration();
                // 标记在倒计时自然结束(还有 1 tick 时)将施加反馈伤害
                if (duration <= 1) {
                    tuanzis_mod$shouldApplyBloodRageFeedback = true;
                } else {
                    // 若又刷新了效果持续时间，则重置标志，防止提前/误触发
                    tuanzis_mod$shouldApplyBloodRageFeedback = false;
                }
                tuanzis_mod$hadBloodRageEffect = true;
            } else {
                // 效果在这个 tick 刚刚结束了
                if (tuanzis_mod$hadBloodRageEffect) {
                    if (tuanzis_mod$shouldApplyBloodRageFeedback && player.isAlive() && !player.level().isClientSide()) {
                        // 施加 2 点 (1 颗心) 的无视护甲/保护反馈伤害
                        var typeHolder = player.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(DamageTypes.GENERIC);
                        BloodRageDamageSource src = new BloodRageDamageSource(typeHolder);

                        // 绝对扣减生命值，以绝对无视护甲与任何效果减免
                        float newHealth = player.getHealth() - 2.0F;
                        player.setHealth(Math.max(0.0F, newHealth));

                        if (newHealth <= 0.0F) {
                            player.die(src);
                        }
                    }
                    // 重置所有标志
                    tuanzis_mod$shouldApplyBloodRageFeedback = false;
                    tuanzis_mod$hadBloodRageEffect = false;
                }
            }
        }
    }
}
