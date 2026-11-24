package me.tuanzi.util;

import me.tuanzi.init.ModStatusEffects;
import me.tuanzi.network.DesperateStandSyncPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * 「背水一战」核心逻辑与纯函数计算工具类。
 */
public final class DesperateStandHelper {
    public static final float MELEE_DAMAGE_BONUS = 0.10f;
    public static final float KILL_MAX_HEALTH_REDUCTION_RATIO = 0.10f;

    private DesperateStandHelper() {
    }

    /**
     * 格式化创伤值显示：小数保留 1 位，整数不显示 .0。
     */
    public static String formatTraumaValue(float trauma) {
        float clamped = Math.max(0.0f, trauma);
        long tenths = Math.round(clamped * 10.0);
        if (tenths % 10L == 0L) {
            return Long.toString(tenths / 10L);
        }
        return String.format(Locale.ROOT, "%.1f", tenths / 10.0);
    }

    /**
     * 计算击杀敌对生物时消减的创伤值（最大血量 * 10%）。
     */
    public static float calcKillTraumaReduction(float killedMaxHealth) {
        if (killedMaxHealth <= 0.0f) {
            return 0.0f;
        }
        return killedMaxHealth * KILL_MAX_HEALTH_REDUCTION_RATIO;
    }

    /**
     * 计算消减后的新创伤值（最低为 0）。
     */
    public static float applyKillReduction(float currentTrauma, float killedMaxHealth) {
        return Math.max(0.0f, currentTrauma - calcKillTraumaReduction(killedMaxHealth));
    }

    /**
     * 判断被击杀实体是否属于敌对生物。
     */
    public static boolean isHostileMob(LivingEntity entity) {
        return entity instanceof Enemy;
    }

    /**
     * 尝试将直接扣血（绕过 hurtServer 的模组反噬/特殊效果伤害）延后记入背水一战创伤值。
     *
     * @return true 表示伤害已成功记入创伤并拦截了即时扣血；false 表示未处于背水一战，按原逻辑扣血。
     */
    public static boolean tryDeferDirectDamage(LivingEntity entity, float amount) {
        if (amount <= 0.0f || entity.level().isClientSide()) {
            return false;
        }
        if (entity instanceof Player player && player.hasEffect(ModStatusEffects.DESPERATE_STAND) && player instanceof DesperateStandTracker tracker) {
            addTrauma(player, tracker, amount, "直接伤害/效果反噬");
            return true;
        }
        return false;
    }

    /**
     * 累加玩家创伤值并同步客户端 HUD。
     */
    public static void addTrauma(Player player, DesperateStandTracker tracker, float amount, String reason) {
        if (amount <= 0.0f) {
            return;
        }
        float oldTrauma = tracker.tuanzis_mod$getDesperateStandTrauma();
        float newTrauma = oldTrauma + amount;
        tracker.tuanzis_mod$setDesperateStandTrauma(newTrauma);
        syncToClient(player, newTrauma);
        ModLog.debug(player, null, "【背水一战】延迟记账(" + reason + ")：新增创伤 "
                + String.format(Locale.ROOT, "%.2f", amount)
                + "，当前总创伤: " + String.format(Locale.ROOT, "%.2f", newTrauma));
    }

    /**
     * 玩家在背水一战期间击杀生物时尝试消减创伤值。
     */
    public static void onPlayerKilledEntity(Player player, LivingEntity killedEntity) {
        if (!player.hasEffect(ModStatusEffects.DESPERATE_STAND) || !isHostileMob(killedEntity)) {
            return;
        }
        if (!(player instanceof DesperateStandTracker tracker)) {
            return;
        }
        float oldTrauma = tracker.tuanzis_mod$getDesperateStandTrauma();
        float maxHp = killedEntity.getMaxHealth();
        float reduction = calcKillTraumaReduction(maxHp);
        float newTrauma = applyKillReduction(oldTrauma, maxHp);
        tracker.tuanzis_mod$setDesperateStandTrauma(newTrauma);
        syncToClient(player, newTrauma);
        ModLog.debug(player, killedEntity, "【背水一战】击杀敌对生物 " + killedEntity.getName().getString()
                + "（最大生命值: " + String.format(Locale.ROOT, "%.1f", maxHp) + "），消减创伤 "
                + String.format(Locale.ROOT, "%.2f", reduction)
                + "，剩余创伤: " + String.format(Locale.ROOT, "%.2f", newTrauma));
    }

    /**
     * 执行最终创伤结算：造成等同于剩余创伤值的真实伤害。
     */
    public static void settleRemainingTrauma(Player player, ServerLevel serverLevel, DesperateStandTracker tracker) {
        float remaining = tracker.tuanzis_mod$getDesperateStandTrauma();
        tracker.tuanzis_mod$setDesperateStandTrauma(0.0f);
        tracker.tuanzis_mod$setDesperateStandSettleTicks(0);
        syncToClient(player, 0.0f);

        if (remaining <= 0.0f || !player.isAlive()) {
            ModLog.debug(player, null, "【背水一战】状态结束：剩余创伤为 0，安全过关！");
            return;
        }

        var typeHolder = player.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.GENERIC);
        DesperateStandDamageSource src = new DesperateStandDamageSource(typeHolder);
        ModLog.debug(player, null, "【背水一战】延迟结算触发！造成真实创伤伤害: " + String.format(Locale.ROOT, "%.2f", remaining));
        player.hurtServer(serverLevel, src, remaining);
    }

    public static void syncToClient(Player player, float trauma) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            ServerPlayNetworking.send(serverPlayer, new DesperateStandSyncPacket(Math.max(0.0f, trauma)));
        }
    }
}
