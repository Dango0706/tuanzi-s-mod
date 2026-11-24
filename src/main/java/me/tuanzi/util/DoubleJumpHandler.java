package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

public class DoubleJumpHandler {

    /**
     * 判断生物是否穿戴具有“二段跳”附魔的靴子
     */
    public static boolean hasDoubleJumpEnchantment(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.enchantment.ItemEnchantments enchantments = boots.getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder : enchantments.keySet()) {
            if (holder.is(ModEnchantments.DOUBLE_JUMP)) {
                return enchantments.getLevel(holder) > 0;
            }
        }
        return false;
    }

    /**
     * 计算玩家的跳跃初始冲量大小，完全对齐 LivingEntity#getJumpPower()。
     * 支持 Attributes.JUMP_STRENGTH 与 MobEffects.JUMP_BOOST 效果加成。
     */
    public static float calculateJumpPower(LivingEntity entity) {
        float jumpBoost = 0.0F;
        if (entity.hasEffect(MobEffects.JUMP_BOOST)) {
            jumpBoost = 0.1F * (entity.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1.0F);
        }
        double jumpStrength = entity.getAttributeValue(Attributes.JUMP_STRENGTH);
        // 空中二段跳方块系数恒为 1.0F
        return (float) jumpStrength + jumpBoost;
    }

    /**
     * 服务端响应二段跳数据包并同步校验
     */
    public static void handleServerDoubleJump(ServerPlayer player) {
        if (player == null || !player.isAlive()) {
            return;
        }

        // 校验靴子是否具有二段跳附魔
        if (!hasDoubleJumpEnchantment(player)) {
            ModLog.debug(String.format("服务端拒绝二段跳: 玩家 %s 靴子未附魔二段跳", player.getName().getString()));
            return;
        }

        // 校验玩家是否处于空中且没有处于非法状态（如鞘翅滑翔、创造飞行等）
        if (player.onGround() || player.isFallFlying() || player.getAbilities().flying || player.getVehicle() != null) {
            ModLog.debug(String.format("服务端拒绝二段跳: 玩家 %s 状态不符 (onGround=%b, isFallFlying=%b, flying=%b)",
                    player.getName().getString(), player.onGround(), player.isFallFlying(), player.getAbilities().flying));
            return;
        }

        // 重置摔落距离
        player.resetFallDistance();

        // 广播粒子与音效给周围其他玩家
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(
                    ParticleTypes.CLOUD,
                    player.getX(), player.getY(), player.getZ(),
                    8, 0.2, 0.05, 0.2, 0.05
            );
            level.playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS,
                    0.8F, 1.25F
            );
        }

        ModLog.debug(String.format("服务端确认玩家 %s 二段跳成功，重置摔落高度，当前位置: (%.2f, %.2f, %.2f)",
                player.getName().getString(), player.getX(), player.getY(), player.getZ()));
    }
}
