package me.tuanzi.mixin;

import me.tuanzi.world.teleport.TeleportManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @org.spongepowered.asm.mixin.Unique
    private boolean tuanzis_mod$hadWolfCommandInOffhand = false;

    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void tuanzis_mod$onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (amount > 0.0f) {
            TeleportManager.onPlayerDamage((ServerPlayer) (Object) this);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tuanzis_mod$tickBerserkCharm(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.level().isClientSide()) return;

        net.minecraft.world.item.ItemStack offhandStack = player.getItemInHand(net.minecraft.world.InteractionHand.OFF_HAND);
        if (offhandStack.is(me.tuanzi.init.ModItems.BERSERK_CHARM)) {
            // 冷却时间检查
            if (!player.getCooldowns().isOnCooldown(offhandStack)) {
                // 生命值低于 30%
                if (player.getHealth() < player.getMaxHealth() * 0.3f) {
                    // 消耗 1 个护符
                    offhandStack.shrink(1);
                    // 获得 15 秒（300 刻）的力量 II（等级 1）与速度 II（等级 1）
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.STRENGTH, 300, 1));
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 300, 1));
                    me.tuanzi.util.ModLog.debug(player, null, "狂暴护符激活！消耗 1 个护符，当前副手剩余: " + offhandStack.getCount() + "，已获得 15 秒的力量 II 与速度 II 效果。");

                    // 进入 15 秒冷却（300 刻）
                    player.getCooldowns().addCooldown(offhandStack, 300);

                    // 播放激活音效：不死图腾使用音效
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.TOTEM_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);

                    // 产生粒子效果
                    if (player.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                            player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
                    }
                }
            }
        }

        // 战狼护符移除副手CD检测
        boolean hasWolfCommand = offhandStack.is(me.tuanzi.init.ModItems.WOLF_COMMAND);
        if (tuanzis_mod$hadWolfCommandInOffhand && !hasWolfCommand) {
            // 移除了，给予 5s (100 ticks) 的 CD 时间
            player.getCooldowns().addCooldown(new net.minecraft.world.item.ItemStack(me.tuanzi.init.ModItems.WOLF_COMMAND), 100);
            
            // 播放一个护符移开的提示音，让反馈更真实
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.5f);
        }
        tuanzis_mod$hadWolfCommandInOffhand = hasWolfCommand;
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void tuanzis_mod$onPlayerDie(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (!player.level().isClientSide() && source.getEntity() instanceof net.minecraft.world.entity.monster.Creeper creeper && creeper.isPowered()) {
            net.minecraft.world.item.ItemStack headStack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PLAYER_HEAD);
            net.minecraft.world.item.component.ResolvableProfile resolvableProfile = net.minecraft.world.item.component.ResolvableProfile.createResolved(player.getGameProfile());
            headStack.set(net.minecraft.core.component.DataComponents.PROFILE, resolvableProfile);
            
            // 输出调试日志
            me.tuanzi.util.ModLog.debug("[玩家头颅掉落] 玩家 " + player.getName().getString() + " 被闪电苦力怕炸死，掉落其正版头颅。");
            
            player.spawnAtLocation((ServerLevel) player.level(), headStack);
        }
    }
}
