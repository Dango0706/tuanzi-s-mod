package me.tuanzi.client;

import me.tuanzi.network.DoubleJumpC2SPacket;
import me.tuanzi.util.DoubleJumpHandler;
import me.tuanzi.util.ModLog;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public final class ClientDoubleJumpHandler {

    private ClientDoubleJumpHandler() {
    }

    /**
     * 客户端执行二段跳
     */
    public static void performClientDoubleJump(LocalPlayer player) {
        float jumpPower = DoubleJumpHandler.calculateJumpPower(player);
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
}
