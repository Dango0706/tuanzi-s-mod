package me.tuanzi.util;

import me.tuanzi.init.ModEnchantments;
import me.tuanzi.network.DoubleJumpC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

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
     * 客户端执行二段跳
     */
    public static void performClientDoubleJump(LocalPlayer player) {
        float jumpPower = calculateJumpPower(player);
        Vec3 currentMovement = player.getDeltaMovement();

        // 垂直速度重置为跳跃冲量大小
        Vec3 newMovement = new Vec3(currentMovement.x, jumpPower, currentMovement.z);

        // 如果在疾跑，增加原版跳跃前向推力
        if (player.isSprinting()) {
            float angle = player.getYRot() * (float) (Math.PI / 180.0);
            newMovement = newMovement.add(-Mth.sin(angle) * 0.2, 0.0, Mth.cos(angle) * 0.2);
        }

        player.setDeltaMovement(newMovement);
        player.resetFallDistance();
        player.needsSync = true;

        // 客户端本地音效与云朵粒子
        player.level().playLocalSound(
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.WIND_CHARGE_THROW, SoundSource.PLAYERS,
                0.8F, 1.25F, false
        );

        for (int i = 0; i < 8; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * 0.4;
            double oz = (player.getRandom().nextDouble() - 0.5) * 0.4;
            player.level().addParticle(
                    ParticleTypes.CLOUD,
                    player.getX() + ox, player.getY(), player.getZ() + oz,
                    -currentMovement.x * 0.15, 0.05, -currentMovement.z * 0.15
            );
        }

        // 发送数据包通知服务端
        ClientPlayNetworking.send(new DoubleJumpC2SPacket());
        ModLog.debug(String.format("客户端玩家 %s 触发二段跳，计算跳跃力度: %s, 当前位置: (%.2f, %.2f, %.2f)",
                player.getName().getString(), jumpPower, player.getX(), player.getY(), player.getZ()));
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
