package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.util.WolfAccessor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public abstract class MobMixin {

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void tuanzis_mod$onDoHurtTarget(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Wolf wolf && (Object) this instanceof WolfAccessor accessor) {
            // 1. 倒地状态无法攻击
            if (accessor.tuanzis_mod$isDowned()) {
                cir.setReturnValue(false);
                return;
            }

            if (wolf.level().isClientSide()) return;

            // 2. 撕裂判定
            if (wolf.isTame() && wolf.getOwner() instanceof Player owner) {
                ItemStack offhandStack = owner.getItemInHand(InteractionHand.OFF_HAND);
                if (offhandStack.is(ModItems.WOLF_COMMAND) && !owner.getCooldowns().isOnCooldown(offhandStack)) {
                    if (target instanceof LivingEntity livingTarget && livingTarget.isAlive()) {
                        if (wolf.getRandom().nextFloat() < 0.15f) {
                            MobEffectInstance currentEffect = livingTarget.getEffect(ModStatusEffects.TEARING);
                            int newAmplifier = 0;
                            if (currentEffect != null) {
                                int currentAmplifier = currentEffect.getAmplifier();
                                newAmplifier = Math.min(3, currentAmplifier + 1); // 最高 4 级 (amplifier 为 3)
                            }
                            // 施加 8 秒 (160 ticks) 的撕裂
                            livingTarget.addEffect(new MobEffectInstance(ModStatusEffects.TEARING, 160, newAmplifier));

                            // 播放撕咬音效（委托给 WolfAccessor）
                            accessor.tuanzis_mod$playGrowlSound(livingTarget);
                            
                            if (wolf.level() instanceof ServerLevel serverLevel) {
                                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                                    livingTarget.getX(), livingTarget.getY() + 0.5, livingTarget.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
                            }
                        }
                    }
                }
            }
        }
    }
}
