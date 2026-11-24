package me.tuanzi.mixin;

import me.tuanzi.init.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 必须保持 priority < 1000（如 900）且 order < 1000（900 / 950），严禁使用 priority >= 1000：
 * sponge-mixin-0.17.4 在 INJECT_PREPARE 阶段为所有 @At("HEAD") 共享绑定原方法首指令 ORIG_0，
 * 随后在 INJECT_APPLY 阶段按 (order 升序, Mixin priority 升序) 执行 insertBefore(ORIG_0, ...)，呈严格 FIFO。
 * 若使用 priority >= 1000，默认 priority=1000 的 BloodRageDamageMixin 与 LivingEntityMixin 会排在本 Mixin 之前执行。
 */
@Mixin(value = LivingEntity.class, priority = 900)
public abstract class SonicBoomBlockMixin {

    @Inject(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            cancellable = true,
            order = 900
    )
    private void tuanzis_mod$redirectSonicBoomHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target instanceof Player player && source.is(DamageTypes.SONIC_BOOM)) {
            if (player.isUsingItem() && player.getUseItem().is(ModItems.ECHO_BREAKER)) {
                // Block successful!
                // 1. Play shield block and metallic chime sounds at player position
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0F, 0.8F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.5F, 1.6F);

                // 2. Spawn explosion and sonic particles
                level.sendParticles(ParticleTypes.EXPLOSION,
                        player.getX(), player.getY() + 1.0, player.getZ(), 3, 0.2, 0.2, 0.2, 0.1);

                if (source.getEntity() instanceof Warden warden) {
                    level.sendParticles(ParticleTypes.SONIC_BOOM,
                            warden.getX(), warden.getY() + 1.0, warden.getZ(), 2, 0.2, 0.2, 0.2, 0.1);

                    // 3. Deal 18 damage to Warden (source is player attack)
                    warden.hurtServer(level, level.damageSources().playerAttack(player), 18.0F);

                    // 4. Knock back Warden
                    Vec3 pushVector = warden.position().subtract(player.position());
                    if (pushVector.lengthSqr() > 1e-4) {
                        pushVector = pushVector.normalize().scale(1.2);
                        warden.push(pushVector.x, 0.4, pushVector.z);
                        warden.syncVelocity = true; // Mark as hurt to sync movement to clients
                    }
                }

                // 5. Damage the Echo Breaker durability (extra 3 durability cost, total 4 damage)
                ItemStack stack = player.getUseItem();
                stack.hurtAndBreak(4, player, player.getUsedItemHand());

                // 5.5. Apply 4 seconds cooldown (80 ticks) for successful block
                player.getCooldowns().addCooldown(stack, 80);

                me.tuanzi.util.ModLog.debug(player, source.getEntity(), "回响破障者声波反弹触发！免疫声波爆破伤害，成功完美格挡并向发射源反弹了 18 点真实伤害和强烈击退效果。");

                // 6. Block the original damage by canceling the call
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @ModifyVariable(
            method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"),
            argsOnly = true,
            order = 950
    )
    private float tuanzis_mod$applyPassiveSonicBoomReduction(float amount, ServerLevel level, DamageSource source) {
        LivingEntity target = (LivingEntity) (Object) this;
        if (target instanceof Player player
                && source.is(DamageTypes.SONIC_BOOM)
                && !(player.isUsingItem() && player.getUseItem().is(ModItems.ECHO_BREAKER))
                && player.getMainHandItem().is(ModItems.ECHO_BREAKER)) {
            float reduced = amount * 0.85F;
            me.tuanzi.util.ModLog.debug(player, source.getEntity(),
                    "回响破障者被动属性触发：主手持有吸收 15% 声波伤害，减免后伤害为 " + String.format("%.2f", reduced) + "。");
            return reduced;
        }
        return amount;
    }
}
