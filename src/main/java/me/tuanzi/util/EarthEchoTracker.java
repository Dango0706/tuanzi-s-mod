package me.tuanzi.util;

import me.tuanzi.init.ModStatusEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 地脉回响静止窗口与冷却恢复增益追踪器
 */
public class EarthEchoTracker {
    public static final Identifier RECOVERY_BOOST_ID = Identifier.fromNamespaceAndPath("tuanzis_mod", "earth_echo_recovery_boost");

    private static class EchoWindow {
        final Vec3 startPos;
        int remainingTicks; // 40 ticks = 2.0s

        EchoWindow(Vec3 startPos) {
            this.startPos = startPos;
            this.remainingTicks = 40;
        }
    }

    private static final Map<UUID, EchoWindow> ACTIVE_WINDOWS = new ConcurrentHashMap<>();
    private static final Map<UUID, Vec3> BUFF_POSITIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> ACTIVE_RECOVERY_BOOST = new ConcurrentHashMap<>();

    /**
     * 定桩重击命中后开启 2.0 秒静止判定窗口
     */
    public static void startWindow(Player player) {
        if (player == null || player.level().isClientSide()) return;
        ACTIVE_WINDOWS.put(player.getUUID(), new EchoWindow(player.position()));
        ModLog.debug(player, null, "【地脉回响】开启 2.0 秒 (40刻) 静止判定窗口，初始坐标: ("
                + String.format("%.2f, %.2f, %.2f", player.getX(), player.getY(), player.getZ()) + ")");
    }

    /**
     * 玩家每 tick 检测位移与静止窗口
     */
    public static void tickPlayer(Player player) {
        if (player == null || player.level().isClientSide()) return;
        UUID uuid = player.getUUID();

        // 1. 静止判定窗口更新
        EchoWindow window = ACTIVE_WINDOWS.get(uuid);
        if (window != null) {
            Vec3 pos = player.position();
            double dx = Math.abs(pos.x - window.startPos.x);
            double dy = Math.abs(pos.y - window.startPos.y);
            double dz = Math.abs(pos.z - window.startPos.z);
            double horizontalDist = Math.hypot(dx, dz);

            // 位移或状态检测：
            // 水平位移 > 0.05 格，或垂直位移 > 0.1 格，或离地/游泳/飞行/乘骑
            boolean moved = horizontalDist > 0.05 || dy > 0.1;
            boolean notGrounded = !player.onGround() && !player.isInWater();
            boolean invalidState = player.isFallFlying() || player.isSwimming() || player.isPassenger();

            if (moved || notGrounded || invalidState) {
                ACTIVE_WINDOWS.remove(uuid);
                ModLog.debug(player, null, "【地脉回响】静止判定中断失败！水平位移: "
                        + String.format("%.4f", horizontalDist)
                        + "，垂直位移: " + String.format("%.4f", dy)
                        + "，离地: " + notGrounded + "，异常状态: " + invalidState);
            } else {
                window.remainingTicks--;
                if (window.remainingTicks <= 0) {
                    ACTIVE_WINDOWS.remove(uuid);
                    // 站定满 2 秒：赋予“地脉回响”增益，锁存 5.0 秒 (100 ticks)
                    player.addEffect(new MobEffectInstance(ModStatusEffects.EARTH_ECHO, 100, 0, false, true, true));
                    BUFF_POSITIONS.put(uuid, player.position());
                    ModLog.debug(player, null, "【地脉回响】原地站定满 2 秒！获得“地脉回响”增益，下一次近战攻击的冷却恢复速度 +10%！锁存 5 秒。");
                }
            }
        }

        // 2. 拥有地脉回响增益时的移动检测（移动则立即失去增益）
        if (player.hasEffect(ModStatusEffects.EARTH_ECHO)) {
            Vec3 buffPos = BUFF_POSITIONS.get(uuid);
            if (buffPos != null) {
                Vec3 currentPos = player.position();
                double dx = Math.abs(currentPos.x - buffPos.x);
                double dy = Math.abs(currentPos.y - buffPos.y);
                double dz = Math.abs(currentPos.z - buffPos.z);
                double dist = Math.hypot(dx, dz);
                if (dist > 0.05 || dy > 0.1 || !player.onGround() || player.isFallFlying() || player.isSwimming() || player.isPassenger()) {
                    player.removeEffect(ModStatusEffects.EARTH_ECHO);
                    BUFF_POSITIONS.remove(uuid);
                    ModLog.debug(player, null, "【地脉回响】检测到玩家移动/离地，立即失去“地脉回响”增益！水平位移: "
                            + String.format("%.4f", dist) + "，垂直位移: " + String.format("%.4f", dy));
                }
            } else {
                BUFF_POSITIONS.put(uuid, player.position());
            }
        } else {
            BUFF_POSITIONS.remove(uuid);
        }

        // 3. 检查冷却恢复加速修饰符：当冷却完全充满时清除
        if (ACTIVE_RECOVERY_BOOST.containsKey(uuid)) {
            if (player.getAttackStrengthScale(0.5f) >= 1.0f) {
                removeRecoveryModifier(player);
                ACTIVE_RECOVERY_BOOST.remove(uuid);
                ModLog.debug(player, null, "【地脉回响】冷却已充满，下一次攻击恢复加速修饰符已移除。");
            }
        }
    }

    /**
     * 玩家近战攻击动作开始时调用（无论命中或挥空）
     */
    public static void onAttackStart(Player player) {
        if (player == null || player.level().isClientSide()) return;
        UUID uuid = player.getUUID();

        // 无论是否命中，若已有“地脉回响”增益，立即消耗该增益，并在该次攻击后的冷却条中享受 +10% 恢复速度
        if (player.hasEffect(ModStatusEffects.EARTH_ECHO)) {
            player.removeEffect(ModStatusEffects.EARTH_ECHO);
            BUFF_POSITIONS.remove(uuid);
            applyRecoveryModifier(player);
            ACTIVE_RECOVERY_BOOST.put(uuid, true);
            ModLog.debug(player, null, "【地脉回响】攻击动作开始，消耗“地脉回响”增益！本次攻击后的冷却恢复享受 +10% 速度。");
        }
    }

    private static void applyRecoveryModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.removeModifier(RECOVERY_BOOST_ID);
            attr.addTransientModifier(new AttributeModifier(
                    RECOVERY_BOOST_ID, 0.10, AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            ));
        }
    }

    private static void removeRecoveryModifier(Player player) {
        AttributeInstance attr = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attr != null) {
            attr.removeModifier(RECOVERY_BOOST_ID);
        }
    }
}
